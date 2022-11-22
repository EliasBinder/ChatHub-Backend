package it.eliasandandrea.chathub.backend.server.rmi;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.ClientDisconnectCallback;
import it.eliasandandrea.chathub.backend.server.ServiceServer;
import it.eliasandandrea.chathub.backend.server.UUIDResolver;
import it.eliasandandrea.chathub.backend.server.rmi.implementations.RmiExchangeImpl;
import it.eliasandandrea.chathub.backend.server.rmi.implementations.RmiHandshakeImpl;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public abstract class RMIServiceServer extends ServiceServer {

    private Registry registry;

    public RMIServiceServer(int port, ClientDisconnectCallback disconnectCallback, UUIDResolver resolver) throws Exception{
        super(port, disconnectCallback);

        registry = LocateRegistry.createRegistry(port);

        Remote handshakeRemote = new RmiHandshakeImpl(responseInterceptors, handlers);
        Naming.rebind("rmi://localhost:" + port + "/handshake", handshakeRemote);

        Remote exchangeRemote = new RmiExchangeImpl(packetInterceptors, responseInterceptors, handlers, resolver);
        Naming.rebind("rmi://localhost:" + port + "/exchange", exchangeRemote);
    }

    @Override
    public void respondWithError(ClientConnection clientConnection, Exception e) {
        // TODO Auto-generated method stub
    }
}
