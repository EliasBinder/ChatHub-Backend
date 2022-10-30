package it.eliasandandrea.chathubbackend.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public final class SocketStreams {

    private SocketStreams() {
    }

    public static byte[] readFully(final Socket socket) {
        try (final BufferedInputStream bis = new BufferedInputStream(socket.getInputStream())) {
            return bis.readAllBytes();
        } catch (IOException e) {
            Log.warning("Could not read from socket", e);
        }
        return null;
    }

    public static Object readObject(final Socket socket) {
        try {
            final ObjectInputStream ois =
                    new ObjectInputStream(socket.getInputStream());
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.warning("Could not read object from socket", e);
        }
        return null;
    }

    public static void writeObject(final Socket socket, final Object object) {
        try (final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            oos.writeObject(object);
        } catch (IOException e) {
            Log.warning("Could not write object to socket", e);
        }
    }

}
