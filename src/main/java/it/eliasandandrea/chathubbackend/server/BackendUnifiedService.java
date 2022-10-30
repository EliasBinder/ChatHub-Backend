package it.eliasandandrea.chathubbackend.server;

import it.eliasandandrea.chathub.model.control.request.Request;
import it.eliasandandrea.chathub.model.control.response.Response;
import it.eliasandandrea.chathub.model.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.model.crypto.Packet;
import it.eliasandandrea.chathubbackend.server.handlers.RequestHandler;
import it.eliasandandrea.chathubbackend.util.Log;
import it.eliasandandrea.chathubbackend.util.ObjectByteConverter;
import it.eliasandandrea.chathubbackend.util.SocketStreams;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendUnifiedService extends ServiceServer {

    private final Map<Class<? extends Request>, RequestHandler<Request, Response>> handlers;
    private final List<PacketInterceptor> packetInterceptors;
    private final List<ResponseInterceptor> responseInterceptors;

    public BackendUnifiedService(int port) throws Exception {
        super(port);
        this.handlers = new HashMap<>();
        this.packetInterceptors = new ArrayList<>();
        this.responseInterceptors = new ArrayList<>();
    }

    @Override
    public Packet onAccepted(Socket socket) {
        Packet packet = (Packet) SocketStreams.readObject(socket);
        if (packet == null) {
            onException(new IOException("Bad input payload"), socket);
            return null;
        }
        for (final PacketInterceptor interceptor : this.packetInterceptors) {
            packet = interceptor.intercept(packet);
        }
        final Request request = (Request) ObjectByteConverter.deserialize(packet.getData());
        if (request == null) {
            onException(new IOException("Bad input payload"), socket);
            return null;
        }
        if (this.handlers.containsKey(request.getClass())) {
            final Response response = this.handlers.get(
                    request.getClass()).handle(socket, request);
            byte[] payload = ObjectByteConverter.serialize(response);

            Packet resPacket = new Packet(payload);
            for (final ResponseInterceptor interceptor : this.responseInterceptors) {
                resPacket = interceptor.intercept(request, resPacket);
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

    @SuppressWarnings("unchecked")
    public <Req extends Request, Res extends Response> void addHandler(final Class<Req> cl, RequestHandler<Req, Res> rh) {
        this.handlers.put(cl, (RequestHandler<Request, Response>) rh);
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
        Packet intercept(Request request, Packet response);
    }
}
