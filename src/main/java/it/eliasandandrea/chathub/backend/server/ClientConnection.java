package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.concurrent.Executors;

public class ClientConnection{

    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;
    private User user;

    public ClientConnection(String username, PublicKey publicKey, Socket socket, DataInputStream inputStream, DataOutputStream outputStream) {
        user = new User(username, publicKey);
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public DataInputStream getInputStream() {
        return inputStream;
    }

    public DataOutputStream getOutputStream() {
        return outputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public User getUser() {
        return user;
    }

    public void sendEvent(ServerEvent event) throws Exception {
        EncryptedObjectPacket toSend = CryptManager.encrypt(event, user.getPublicKey());
        if (outputStream != null)
            Executors.newSingleThreadExecutor().submit(() -> SocketStreams.writeObject(outputStream, toSend));
    }

}
