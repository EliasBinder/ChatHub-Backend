package it.eliasandandrea.chathub.model.crypto;

import java.io.Serializable;

public class Packet implements Serializable {

    private final byte[] data;

    public Packet(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
