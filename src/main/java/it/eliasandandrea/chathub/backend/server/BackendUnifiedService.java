package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.util.Log;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendUnifiedService extends ServiceServer {

    private final Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers;
    private final List<PacketInterceptor> packetInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    public BackendUnifiedService(int port) throws Exception {
        super(port);
        this.handlers = new HashMap<>();
        this.packetInterceptors = new ArrayList<>();
        this.responseInterceptors = new ArrayList<>();
    }

    @Override
    public Packet onAccepted(Socket socket, DataInputStream din, DataOutputStream dos) throws IOException {
        Packet packet = (Packet) SocketStreams.readObject(din);
        if (packet == null) {
            onException(new IOException("Bad input payload"), socket);
            throw new IOException("Bad input payload");
        }
        for (final PacketInterceptor interceptor : this.packetInterceptors) {
            packet = interceptor.intercept(packet);
        }
        final ClientEvent request = (ClientEvent) packet.getSerializable();
        if (request == null) {
            onException(new IOException("Bad input payload"), socket);
            return null;
        }
        if (this.handlers.containsKey(request.getClass())) {
            final ServerEvent response = this.handlers.get(
                    request.getClass()).handle(socket, din, dos, request);
            byte[] payload = ObjectByteConverter.serialize(response);
            Packet resPacket = new Packet(payload);
            for (final ResponseInterceptor interceptor : this.responseInterceptors) {
                resPacket = interceptor.intercept(socket, request, resPacket);
            }
            return resPacket;
        }
        return null;
    }

    @Override
    public void onException(Exception e, Socket socket) {
        if (socket != null) {
            respondWithError(socket, e);
        }
        Log.warning("exception in BackendUnifiedService", e);
    }

    public void addHandler(final Class<? extends ClientEvent> cl, RequestHandler rh) {
        this.handlers.put(cl, (RequestHandler<ClientEvent, ServerEvent>) rh);
    }

    public void addPacketInterceptor(final PacketInterceptor packetInterceptor) {
        this.packetInterceptors.add(packetInterceptor);
    }

    public void addResponseInterceptor(final ResponseInterceptor responseInterceptor) {
        this.responseInterceptors.add(responseInterceptor);
    }

    public interface PacketInterceptor {
        Packet intercept(Packet packet);
    }
    public interface ResponseInterceptor {
        Packet intercept(Socket socket, ClientEvent request, Packet response);
    }
}
