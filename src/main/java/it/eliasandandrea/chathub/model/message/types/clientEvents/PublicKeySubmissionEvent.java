package it.eliasandandrea.chathub.model.message.types.clientEvents;

import it.eliasandandrea.chathub.model.message.ClientEvent;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class PublicKeySubmissionEvent implements ClientEvent, Serializable {

    public byte[] publicKey;
    public String username;

    public PublicKeySubmissionEvent(PublicKey publicKey) {
        this.publicKey = publicKey.getEncoded();
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
    }

}
