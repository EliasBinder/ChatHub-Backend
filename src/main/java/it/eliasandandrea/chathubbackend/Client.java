package it.eliasandandrea.chathubbackend;

import it.eliasandandrea.chathubbackend.encryption.Keystore;
import it.eliasandandrea.chathubbackend.encryption.ObjectByteConverter;
import it.eliasandandrea.chathubbackend.encryption.RSACipher;
import it.eliasandandrea.chathub.model.message.types.clientEvents.PublicKeySubmissionEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class Client {

    public static HashMap<String, Client> clients = new HashMap<>();

    private String uuid;
    private DataInputStream in;
    private DataOutputStream out;
    private PublicKey publicKey;

    public Client(Socket connection, String uuid) throws IOException {
        this.uuid = uuid;
        in = new DataInputStream(connection.getInputStream());
        out = new DataOutputStream(connection.getOutputStream());
        Executors.newSingleThreadExecutor().submit(() -> {
            boolean handshake = true;
            while(true){
                // Read the message length
                int lengthInt = in.readInt();
                // Create byte array from reading bytes
                byte[] bytes = in.readNBytes(lengthInt);
                if (handshake) {
                    try {
                        PublicKeySubmissionEvent event = (PublicKeySubmissionEvent) ObjectByteConverter.deserialize(bytes);
                        publicKey = event.getPublicKey();
                        handshake = false;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    // Decrypt the message
                    bytes = RSACipher.decrypt(bytes, Keystore.getInstance().getPrivateKey("server"));
                    //Convert decrypted message to object and execute callback
                    //TODO
                }
            }
        });
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}
