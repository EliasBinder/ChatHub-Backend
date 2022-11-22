package it.eliasandandrea.chathub.backend.server.tcp;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

public class TCPClientConnection extends ClientConnection {

    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;

    public TCPClientConnection(User user, Socket socket, DataInputStream inputStream, DataOutputStream outputStream) {
        super(user);
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
