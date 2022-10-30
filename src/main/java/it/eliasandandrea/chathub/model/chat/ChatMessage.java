package it.eliasandandrea.chathub.model.chat;

public interface ChatMessage {
    String getReceiver();
    byte[] getContent();
}
