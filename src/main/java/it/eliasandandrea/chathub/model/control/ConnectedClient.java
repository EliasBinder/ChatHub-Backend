package it.eliasandandrea.chathub.model.control;

import java.net.InetAddress;
import java.security.PublicKey;

public class ConnectedClient {

    private InetAddress address;
    private String username;
    private String uuid;
    private PublicKey publicKey;

    public ConnectedClient(InetAddress address, String username, String uuid, PublicKey publicKey) {
        this.address = address;
        this.username = username;
        this.uuid = uuid;
        this.publicKey = publicKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public InetAddress getAddress() {
        return address;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getUuid() {
        return uuid;
    }
}
