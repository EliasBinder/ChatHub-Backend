package it.eliasandandrea.chathubbackend.encryption;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
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

public class RSACipher {

    private static final int SALT_ROUNDS = 20;

    public static void init(Path publicKeyPath, Path privateKeyPath, String password) throws Exception {
        if (!publicKeyPath.toFile().exists() || !privateKeyPath.toFile().exists()) {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            final KeyPair pair = generator.generateKeyPair();
            writeKeyToFile(pair.getPublic().getEncoded(), publicKeyPath);
            writeKeyToFile(encryptPrivateKey(pair.getPrivate(), password), privateKeyPath);
        }
    }

    public RSACipher(Path pub, Path priv, String password) throws Exception {
        byte[] encodedPub = readKeyFromFile(pub);
        byte[] encodedPriv = readKeyFromFile(priv);

        PublicKey publicKey = bytesToPublicKey(encodedPub);
        PrivateKey privateKey = decryptPrivateKey(encodedPriv, password);

        Keystore.getInstance().addKey("server", publicKey, privateKey);
    }


    public static byte[] encrypt(Object message, PublicKey publicKey) throws IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        Cipher encryptor = Cipher.getInstance("RSA");
        encryptor.init(Cipher.ENCRYPT_MODE, publicKey);
        return encryptor.doFinal(ObjectByteConverter.serialize(message));
    }

    public static byte[] decrypt(byte[] message, PrivateKey privateKey) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher decryptor = Cipher.getInstance("RSA");
        decryptor.init(Cipher.DECRYPT_MODE, privateKey);
        return decryptor.doFinal(message);
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
