package it.eliasandandrea.chathub.model.control.response;

import java.security.PublicKey;

public class JoinServerResponse implements Response {

    private final String uuid;
    private final PublicKey serverPublicKey;

    public JoinServerResponse(String uuid, PublicKey serverPublicKey) {
        this.uuid = uuid;
        this.serverPublicKey = serverPublicKey;
    }

}
