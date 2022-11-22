package it.eliasandandrea.chathub.backend.server.rmi;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.rmi.MessageCallbackEvent;

public class RMIClientConnection extends ClientConnection {

    public MessageCallbackEvent callbackEvent;

    public RMIClientConnection(User user, MessageCallbackEvent callbackEvent) {
        super(user);
        this.callbackEvent = callbackEvent;
    }

    @Override
    public void sendEvent(ServerEvent event) throws Exception {
        EncryptedObjectPacket toSend = CryptManager.encrypt(event, user.getPublicKey());
        if (callbackEvent != null)
            callbackEvent.sendAsyncMessage(toSend);
    }
}
