package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.Error;
import it.eliasandandrea.chathub.shared.util.Log;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public abstract class ServiceServer {

    private final ServerSocket socket;

    public ServiceServer(int port) throws Exception {
        this.socket = new ServerSocket(port);
    }

    public void waitForConnection() {
        try {
            final Socket client = this.socket.accept();
            Log.info("accepted from " + client.getInetAddress());
            Executors.newSingleThreadExecutor().submit(() -> {
                while (client.isConnected()){
                    try{
                        Packet response = this.onAccepted(client);
                        if (response != null) {
                            Log.info("writing response");
                            SocketStreams.writeObject(client, response);
                        }
                    } finally {
                        try {
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        } catch (IOException e) {
            this.onException(e, null);
        }
    }

    public static void respondWithError(Socket socket, Exception e) {
        try {
            byte[] payload = ObjectByteConverter.serialize(
                    new Error(e.getClass().getSimpleName(), e.getMessage()));
            if (payload != null) {
                socket.getOutputStream().write(payload);
                socket.getOutputStream().close();
            } else {
                socket.close();
            }
        } catch (IOException ie) {
            Log.warning("Could not respond with error", ie);
        }
    }

    public abstract Packet onAccepted(Socket socket);

    public abstract void onException(Exception e, Socket socket);
}
