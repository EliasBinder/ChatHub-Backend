package it.eliasandandrea.chathubbackend.server.handlers;

import it.eliasandandrea.chathub.model.control.request.JoinServerRequest;
import it.eliasandandrea.chathub.model.control.response.JoinServerResponse;

import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;

public class JoinServerHandler implements RequestHandler<JoinServerRequest, JoinServerResponse> {

    private final OnNewClientJoinedCallback callback;

    private final PublicKey serverPublicKey;

    public JoinServerHandler(PublicKey publicKey, OnNewClientJoinedCallback callback) {
        this.serverPublicKey = publicKey;
        this.callback = callback;
    }

    @Override
    public JoinServerResponse handle(Socket socket, JoinServerRequest payload) {
        if (this.callback != null) {
            String uuid = this.callback.onJoin(socket.getInetAddress(),
                    payload.getUsername(), payload.getUserPublicKey());
            payload.setUuid(uuid);
            return new JoinServerResponse(uuid, this.serverPublicKey);
        }
        return null;
    }

    public interface OnNewClientJoinedCallback {
        String onJoin(InetAddress client, String username, PublicKey publicKey);
    }
}
