package it.eliasandandrea.chathubbackend.server;

import it.eliasandandrea.chathub.model.control.response.ErrorResponse;
import it.eliasandandrea.chathub.model.crypto.Packet;
import it.eliasandandrea.chathubbackend.util.Log;
import it.eliasandandrea.chathubbackend.util.ObjectByteConverter;
import it.eliasandandrea.chathubbackend.util.SocketStreams;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class ServiceServer {

    private final ServerSocket socket;

    public ServiceServer(int port) throws Exception {
        this.socket = new ServerSocket(port);
    }

    public void waitForConnection() {
        try {
            final Socket client = this.socket.accept();
            Log.info("accepted from " + client.getInetAddress());
            Packet response = this.onAccepted(client);
            if (response != null) {
                Log.info("writing response");
                SocketStreams.writeObject(client, response);
            }
        } catch (IOException e) {
            this.onException(e, null);
        }
    }

    public static void respondWithError(Socket socket, Exception e) {
        try {
            byte[] payload = ObjectByteConverter.serialize(
                    new ErrorResponse(e.getClass().getSimpleName(), e.getMessage()));
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
