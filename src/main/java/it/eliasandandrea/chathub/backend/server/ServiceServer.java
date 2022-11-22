package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.backend.server.tcp.TCPUnifiedService;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ServiceServer {

    protected int port;

    protected final Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers;
    protected final List<PacketInterceptor> packetInterceptors;
    protected final List<ResponseInterceptor> responseInterceptors;

    protected ClientDisconnectCallback disconnectCallback;

    public ServiceServer(int port, ClientDisconnectCallback disconnectCallback) {
        this.port = port;
        this.disconnectCallback = disconnectCallback;
        this.handlers = new HashMap<>();
        this.packetInterceptors = new ArrayList<>();
        this.responseInterceptors = new ArrayList<>();
    }

    public abstract void respondWithError(ClientConnection clientConnection, Exception e);

    public abstract Packet onAccepted(ClientConnection clientConnection) throws IOException;

    public abstract void onConnectionClose(ClientConnection clientConnection);

    public void addHandler(final Class<? extends ClientEvent> cl, RequestHandler rh) {
        this.handlers.put(cl, (RequestHandler<ClientEvent, ServerEvent>) rh);
    }

    public void addPacketInterceptor(final PacketInterceptor packetInterceptor) {
        this.packetInterceptors.add(packetInterceptor);
    }

    public void addResponseInterceptor(final ResponseInterceptor responseInterceptor) {
        this.responseInterceptors.add(responseInterceptor);
    }
}
