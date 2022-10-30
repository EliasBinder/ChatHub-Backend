package it.eliasandandrea.chathub.model.message.types.clientEvents;

import it.eliasandandrea.chathub.model.message.ClientEvent;

public class SendEncryptedMessageEvent implements ClientEvent {

    public String receiver;
    public byte[] encryptedMessageObject; //encrypted <? implements Message> object

}
