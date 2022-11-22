package it.eliasandandrea.chathub.backend.server.tcp;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.ClientDisconnectCallback;
import it.eliasandandrea.chathub.backend.server.PacketInterceptor;
import it.eliasandandrea.chathub.backend.server.ResponseInterceptor;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.ChatEntityRemovedEvent;
import it.eliasandandrea.chathub.shared.util.Log;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;
import it.eliasandandrea.chathub.shared.util.SocketStreams;

import java.io.EOFException;
import java.io.IOException;

public class TCPUnifiedService extends TCPServiceServer {

    public TCPUnifiedService(int port, ClientDisconnectCallback disconnectCallback) throws Exception {
        super(port, disconnectCallback);
    }

    @Override
    public Packet onAccepted(ClientConnection clientConnection) throws IOException, EOFException {
        TCPClientConnection tcpClientConnection = (TCPClientConnection) clientConnection;
        Packet packet = null;
        try {
            packet = (Packet) SocketStreams.readObject(tcpClientConnection.getInputStream());
        }catch (Exception e) {
        }
        if (packet == null) {
            onException(new IOException("Bad input payload"), tcpClientConnection);
        }
        for (final PacketInterceptor interceptor : this.packetInterceptors) {
            packet = interceptor.intercept(packet);
        }
        final ClientEvent request = (ClientEvent) packet.getSerializable();
        if (request == null) {
            onException(new IOException("Bad input payload"), tcpClientConnection);
            return null;
        }
        if (this.handlers.containsKey(request.getClass())) {
            final ServerEvent response = this.handlers.get(
                    request.getClass()).handle(clientConnection, request);
            byte[] payload = ObjectByteConverter.serialize(response);
            Packet resPacket = new Packet(payload);
            for (final ResponseInterceptor interceptor : this.responseInterceptors) {
                resPacket = interceptor.intercept(clientConnection, request, resPacket);
            }
            return resPacket;
        }
        return null;
    }

    @Override
    public void onConnectionClose(ClientConnection clientConnection) {
        disconnectCallback.onDisconnect(clientConnection);
    }

    public void onException(Exception e, ClientConnection clientConnection) {
        if (clientConnection != null) {
            respondWithError(clientConnection, e);
        }
    }
}
