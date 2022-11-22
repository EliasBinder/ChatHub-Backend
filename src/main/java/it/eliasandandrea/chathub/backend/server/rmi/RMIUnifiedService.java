package it.eliasandandrea.chathub.backend.server.rmi;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.ClientDisconnectCallback;
import it.eliasandandrea.chathub.backend.server.UUIDResolver;
import it.eliasandandrea.chathub.shared.crypto.Packet;

import java.io.IOException;

public class RMIUnifiedService extends RMIServiceServer{

    public RMIUnifiedService(int port, ClientDisconnectCallback disconnectCallback, UUIDResolver resolver) throws Exception {
        super(port, disconnectCallback, resolver);
    }

    @Override
    public Packet onAccepted(ClientConnection clientConnection) throws IOException {
        //Not needed here because unlike in tcp, here the rmiregistery handles the connection
        return null;
    }

    @Override
    public void onConnectionClose(ClientConnection clientConnection) {
        disconnectCallback.onDisconnect(clientConnection);
    }
}
