package it.eliasandandrea.chathub.model.crypto;

import it.eliasandandrea.chathub.model.control.response.Response;
import it.eliasandandrea.chathubbackend.util.ObjectByteConverter;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptManager {

    private static final int SALT_ROUNDS = 20;

    private static CryptManager singleton;
    public static CryptManager getInstance() {
        return singleton;
    }

    public static void init(Path publicKeyPath, Path privateKeyPath, String password) throws Exception {
        if (!publicKeyPath.toFile().exists() || !privateKeyPath.toFile().exists()) {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            final KeyPair pair = generator.generateKeyPair();
            writeKeyToFile(pair.getPublic().getEncoded(), publicKeyPath);
            writeKeyToFile(encryptPrivateKey(pair.getPrivate(), password), privateKeyPath);
        }
        singleton = new CryptManager(publicKeyPath, privateKeyPath, password);
    }

    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final Cipher decryptionCipher;

    public CryptManager(Path pub, Path priv, String password) throws Exception {
        byte[] encodedPub = readKeyFromFile(pub);
        byte[] encodedPriv = readKeyFromFile(priv);

        this.publicKey = bytesToPublicKey(encodedPub);
        this.privateKey = decryptPrivateKey(encodedPriv, password);

        this.decryptionCipher = Cipher.getInstance("RSA");
        this.decryptionCipher.init(Cipher.DECRYPT_MODE, this.privateKey);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public static EncryptedObjectPacket encrypt(Response response, PublicKey publicKey) throws IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        final byte[] serialized = ObjectByteConverter.serialize(response);
        if (serialized == null) {
            return null;
        }
        return encrypt(serialized, publicKey);
    }

    public static EncryptedObjectPacket encrypt(byte[] response, PublicKey publicKey) throws IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        final KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(128);

        final SecretKey symmetricKey = aesGen.generateKey();
        final Cipher dataEncryptor = Cipher.getInstance("AES");
        dataEncryptor.init(Cipher.ENCRYPT_MODE, symmetricKey);
        response = dataEncryptor.doFinal(response);

        final Cipher keyEncryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        keyEncryptor.init(Cipher.PUBLIC_KEY, publicKey);
        final byte[] encryptedSymmetricKey = keyEncryptor.doFinal(symmetricKey.getEncoded());
        return new EncryptedObjectPacket(response, encryptedSymmetricKey);
    }

    public Object decryptToObject(EncryptedObjectPacket packet) throws IllegalBlockSizeException, BadPaddingException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        return ObjectByteConverter.deserialize(decrypt(packet));
    }

    public byte[] decrypt(EncryptedObjectPacket packet) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (packet == null) {
            return null;
        }
        final Cipher keyDecryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        keyDecryptor.init(Cipher.PRIVATE_KEY, this.privateKey);
        final SecretKey symmetricKey = new SecretKeySpec(
                keyDecryptor.doFinal(packet.getEncryptedSymmetricKey()), "AES");
        final Cipher dataDecryptor = Cipher.getInstance("AES");
        dataDecryptor.init(Cipher.DECRYPT_MODE, symmetricKey);
        return dataDecryptor.doFinal(packet.getEncryptedData());
    }

    private static byte[] encryptPrivateKey(PrivateKey privateKey, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException,
            IOException, InvalidAlgorithmParameterException, InvalidParameterSpecException {
        final SecureRandom secr = new SecureRandom();
        byte[] salt = new byte[8];
        secr.nextBytes(salt);

        final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, SALT_ROUNDS);
        final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, SALT_ROUNDS, 128);
        final SecretKeyFactory keyFactory =
                SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");
        final SecretKey pbeKey = keyFactory.generateSecret(keySpec);

        final Cipher pbeCipher = Cipher.getInstance("PBEWithSHA1AndDESede");
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, parameterSpec);
        byte[] encr = pbeCipher.doFinal(privateKey.getEncoded());

        final AlgorithmParameters algoParams = AlgorithmParameters.getInstance("PBEWithSHA1AndDESede");
        algoParams.init(parameterSpec);

        return new EncryptedPrivateKeyInfo(algoParams, encr).getEncoded();
    }

    private static PrivateKey decryptPrivateKey(byte[] encPrivKey, String password) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        final EncryptedPrivateKeyInfo encPrivKeyInfo = new EncryptedPrivateKeyInfo(encPrivKey);
        final Cipher cipher = Cipher.getInstance(encPrivKeyInfo.getAlgName());

        final PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encPrivKeyInfo.getAlgName());
        Key pbeKey = keyFactory.generateSecret(pbeKeySpec);

        final AlgorithmParameters algParams = encPrivKeyInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);

        final KeySpec privKeySpec = encPrivKeyInfo.getKeySpec(cipher);
        final KeyFactory privKeyFactory = KeyFactory.getInstance("RSA");

        return privKeyFactory.generatePrivate(privKeySpec);
    }

    private static void writeKeyToFile(byte[] bb, Path filepath) throws Exception {
        final FileOutputStream fos = new FileOutputStream(filepath.toFile());
        fos.write(Base64.getEncoder().encode(bb));
        fos.close();
    }

    private static byte[] readKeyFromFile(Path filepath) throws Exception {
        return Base64.getDecoder().decode(
                new BufferedInputStream(
                        new FileInputStream(filepath.toFile())).readAllBytes());
    }

    private PublicKey bytesToPublicKey(byte[] bb) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bb);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }
}
