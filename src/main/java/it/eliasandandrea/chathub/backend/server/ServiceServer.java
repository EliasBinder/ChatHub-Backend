package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.Error;
import it.eliasandandrea.chathub.shared.util.Log;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Extensible class to provide multithreaded server capabilities. The actual functionality
 * is provided using the onAccepted method for processing an incoming connection and generating
 * a response and the onException method for handling errors that arose during the process.
 * <p>
 * A ServerSocket is set up to publish the service on a local network port, specified on
 * initialization.
 * <p>
 * The incoming and outgoing data streams for this class are intended to be POJOs, handled using
 * DataInput/OutputStream objects.
 * <p>
 * All established connections are kept open until explicitly closed by the client or the server,
 * or until an Exception is thrown.
 * <p>
 * All newly accepted incoming connections are handled in a dedicated thread.
 *
 * @see ServerSocket the underlying socket
 * @see Executors#newSingleThreadExecutor() used for handling connections in separate threads
 */
public abstract class ServiceServer {

    private final ServerSocket socket;

    /**
     * Initialize the service, which will be available through a ServerSocket at the specified
     * port on the local network.
     *
     * @param port to publish the service on
     * @throws Exception in case the port is taken or another socket-related error occurs
     */
    public ServiceServer(int port) throws Exception {
        this.socket = new ServerSocket(port);
    }

    /**
     * Blocking method to let the server hang and wait for an incoming connection, then
     * configuring and bootstrapping a thread that handles the established connection.
     * Once a socket is opened, DataInput/OutputStreams are used to convert the in/outgoing
     * streams to/from POJOs and the connection is kept open.
     * The onAccepted implementation is used to generate a response Packet for each
     * incoming stream that can be successfully parsed.
     *
     * @see #onAccepted(Socket, DataInputStream, DataOutputStream) of this class's implementation
     */
    public void waitForConnection() {
        try {
            final Socket client = this.socket.accept();
            final DataInputStream dis = new DataInputStream(client.getInputStream());
            final DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            Log.info("accepted from " + client.getInetAddress());
            Executors.newSingleThreadExecutor().submit(() -> {
                boolean connected = true;
                while (connected){
                    try{
                        Packet response = this.onAccepted(client, dis, dos);
                        if (response != null) {
                            Log.info("writing response");
                            SocketStreams.writeObject(dos, response);
                        }
                    } catch (Exception ex){
                        connected = false;
                        Log.error(ex.getMessage());
                        onException(ex, client);
                    }
                }
            });
        } catch (Exception e) {
            this.onException(e, null);
        }
    }

    /**
     * Convenience method to send the payload corresponding to an error that occurred back to
     * the connected client.
     * The corresponding socket is closed after the payload has been sent.
     * @param socket established with the client, to which the error is related; the socket
     *               must be open
     * @param e the occurred Exception to be transmitted
     */
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

    public abstract Packet onAccepted(Socket socket, DataInputStream dis, DataOutputStream dos) throws IOException;

    public abstract void onException(Exception e, Socket socket);
}
