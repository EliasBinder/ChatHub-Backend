package it.eliasandandrea.chathub.backend.server.handlers;

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

    public HandshakeHandler(PublicKey publicKey) {
        this.serverPublicKey = publicKey;
    }

    @Override
    public HandshakeResponseEvent handle(Socket socket, ClientEvent payload) {
        HandshakeRequestEvent handshakeRequestEvent = (HandshakeRequestEvent) payload;
        try {
            User newUser = new User("", ((HandshakeRequestEvent) payload).getPublicKey());
            System.out.println("Byte length of new users public key: " + newUser.publicKey.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HandshakeResponseEvent handshakeResponseEvent = new HandshakeResponseEvent();
        handshakeResponseEvent.uuid = UUID.randomUUID().toString();
        handshakeResponseEvent.chats = new LinkedList<>(){{
            add(new User("TestUser1", null));
            add(new User("TestUser2", null));
            add(new User("TestUser3", null));
        }};
        return new HandshakeResponseEvent();
    }
}
