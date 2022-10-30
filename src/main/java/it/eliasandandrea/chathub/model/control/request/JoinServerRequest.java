package it.eliasandandrea.chathub.model.control.request;

import java.security.PublicKey;

public class JoinServerRequest extends AuthenticatedRequest {

    private String username;
    private PublicKey userPublicKey;

    public JoinServerRequest(String username, PublicKey publicKey) {
        super(null);
        this.username = username;
        this.userPublicKey = publicKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserPublicKey(PublicKey userPublicKey) {
        this.userPublicKey = userPublicKey;
    }

    public String getUsername() {
        return username;
    }

    public PublicKey getUserPublicKey() {
        return userPublicKey;
    }
}
