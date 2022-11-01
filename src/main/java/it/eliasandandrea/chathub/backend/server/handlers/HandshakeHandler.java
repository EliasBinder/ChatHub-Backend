package it.eliasandandrea.chathub.backend.server.handlers;

import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.model.ChatEntity;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.HandshakeResponseEvent;

import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.UUID;

public class HandshakeHandler implements RequestHandler<ClientEvent, ServerEvent> {

    private final PublicKey serverPublicKey;
    private NewUserCallback callback;

    public HandshakeHandler(PublicKey publicKey, NewUserCallback callback) {
        this.serverPublicKey = publicKey;
        this.callback = callback;
    }

    @Override
    public ServerEvent handle(Socket socket, ClientEvent payload) {
        HandshakeRequestEvent handshakeRequestEvent = (HandshakeRequestEvent) payload;
        User newUser;
        try {
            newUser = new User("", handshakeRequestEvent.getPublicKey());
            newUser.UUID = UUID.randomUUID().toString();
            callback.onNewUser(socket, newUser);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        HandshakeResponseEvent handshakeResponseEvent = new HandshakeResponseEvent();
        handshakeResponseEvent.uuid = newUser.getUUID();
        handshakeResponseEvent.serverPublicKey = CryptManager.publicKeyToBytes(serverPublicKey);
        handshakeResponseEvent.chats = new ChatEntity[]{
            new User("TestUser1", null),
            new User("TestUser2", null),
            new User("TestUser3", null),
        };
        return handshakeResponseEvent;
    }
}
