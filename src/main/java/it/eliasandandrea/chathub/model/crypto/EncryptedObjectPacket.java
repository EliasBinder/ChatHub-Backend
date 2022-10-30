package it.eliasandandrea.chathub.model.crypto;

public class EncryptedObjectPacket extends Packet {

    private final byte[] encryptedSymmetricKey;

    public EncryptedObjectPacket(byte[] encryptedData, byte[] encryptedSymmetricKey) {
        super(encryptedData);
        this.encryptedSymmetricKey = encryptedSymmetricKey;
    }

    public byte[] getEncryptedData() {
        return getData();
    }

    public byte[] getEncryptedSymmetricKey() {
        return encryptedSymmetricKey;
    }
}
