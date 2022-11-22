package it.eliasandandrea.chathub.backend.server.rmi.implementations;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.ResponseInterceptor;
import it.eliasandandrea.chathub.backend.server.rmi.RMIClientConnection;
import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.protocol.rmi.MessageCallbackEvent;
import it.eliasandandrea.chathub.shared.protocol.rmi.RMIHandshake;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class RmiHandshakeImpl extends UnicastRemoteObject implements RMIHandshake {

    private Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers;
    private List<ResponseInterceptor> responseInterceptors;

    public RmiHandshakeImpl(
            List<ResponseInterceptor> responseInterceptors,
            Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers
    ) throws RemoteException {
        this.responseInterceptors = responseInterceptors;
        this.handlers = handlers;
    }

    @Override
    public Packet doHandshake(HandshakeRequestEvent handshakeRequestEvent, MessageCallbackEvent messageCallbackEvent) {
        ClientConnection clientConnection = new RMIClientConnection(null, messageCallbackEvent);
        if (this.handlers.containsKey(HandshakeRequestEvent.class)) {
            final ServerEvent response = this.handlers.get(
                    HandshakeRequestEvent.class).handle(clientConnection, handshakeRequestEvent);
            byte[] payload = ObjectByteConverter.serialize(response);
            Packet resPacket = new Packet(payload);
            for (final ResponseInterceptor interceptor : this.responseInterceptors) {
                resPacket = interceptor.intercept(clientConnection, handshakeRequestEvent, resPacket);
            }
            return resPacket;
        }
        return null;
    }

}
