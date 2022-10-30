package it.eliasandandrea.chathub.model.chat;

/**
 * Wrapper for encrypted message, bearing the receiver's identity so that
 * it can be correctly routed by the server without decrypting the content.
 */
public class EncryptedMessage implements ChatMessage {

    private final String receiver;
    private final byte[] encrypted;

    public EncryptedMessage(String receiver, byte[] encrypted) {
        this.receiver = receiver;
        this.encrypted = encrypted;
    }

    @Override
    public String getReceiver() {
        return this.receiver;
    }

    public byte[] getContent() {
        return encrypted;
    }
}
