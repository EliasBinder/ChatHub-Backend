package it.eliasandandrea.chathub.backend.server.rmi.implementations;

import it.eliasandandrea.chathub.backend.server.PacketInterceptor;
import it.eliasandandrea.chathub.backend.server.ResponseInterceptor;
import it.eliasandandrea.chathub.backend.server.UUIDResolver;
import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.protocol.rmi.RMIExchange;
import it.eliasandandrea.chathub.shared.util.ObjectByteConverter;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class RmiExchangeImpl extends UnicastRemoteObject implements RMIExchange {

    private Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers;
    private List<PacketInterceptor> packetInterceptors;
    private List<ResponseInterceptor> responseInterceptors;
    private UUIDResolver uuidResolver;

    public RmiExchangeImpl(
            List<PacketInterceptor> packetInterceptors,
            List<ResponseInterceptor> responseInterceptors,
            Map<Class<? extends ClientEvent>, RequestHandler<ClientEvent, ServerEvent>> handlers,
            UUIDResolver resolver
    ) throws RemoteException {
        this.handlers = handlers;
        this.packetInterceptors = packetInterceptors;
        this.responseInterceptors = responseInterceptors;
        this.uuidResolver = resolver;
    }

    @Override
    public Packet sendMessage(Packet encryptedObjectPacket, String senderUUID) {
        Packet packet = encryptedObjectPacket;
        for (PacketInterceptor packetInterceptor : packetInterceptors) {
            packet = packetInterceptor.intercept(packet);
        }
        ClientEvent clientEvent = (ClientEvent) packet.getSerializable();
        if (clientEvent == null) {
            return null;
        }
        if (this.handlers.containsKey(clientEvent.getClass())) {
            final ServerEvent response = this.handlers.get(
                    HandshakeRequestEvent.class).handle(uuidResolver.resolve(senderUUID), clientEvent); //TODO: handle exception sender does not exist
            byte[] payload = ObjectByteConverter.serialize(response);
            Packet resPacket = new Packet(payload);
            for (final ResponseInterceptor interceptor : this.responseInterceptors) {
                resPacket = interceptor.intercept(uuidResolver.resolve(senderUUID), clientEvent, resPacket); //TODO: handle exception sender does not exist
            }
            return resPacket;
        }
        return null;
    }
}
