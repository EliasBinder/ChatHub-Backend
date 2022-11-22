package it.eliasandandrea.chathub.backend;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.ClientDisconnectCallback;
import it.eliasandandrea.chathub.backend.server.ServiceServer;
import it.eliasandandrea.chathub.backend.server.UUIDResolver;
import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.backend.server.rmi.RMIUnifiedService;
import it.eliasandandrea.chathub.backend.server.tcp.TCPUnifiedService;
import it.eliasandandrea.chathub.backend.server.tcp.TCPClientConnection;
import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.model.Group;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.SetUsernameEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.ChangeUsernameEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.ChatEntityAddedEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.ChatEntityRemovedEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.HandshakeResponseEvent;
import it.eliasandandrea.chathub.shared.protocol.sharedEvents.MessageEvent;
import it.eliasandandrea.chathub.shared.util.LocalPaths;
import it.eliasandandrea.chathub.shared.util.Log;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServerImpl {

    private final List<ClientConnection> clients;
    private final List<Group> groups;
    private CryptManager cryptManager;

    public ServerImpl(String password) throws Exception {
        this.clients = new LinkedList<>();
        this.groups = new LinkedList<>();
        Path dataDir = LocalPaths.getData();

        //Server key pair
        Path serverPublicKeyPath = dataDir.resolve("id_chathub_srv");
        Path serverPrivateKeyPath = dataDir.resolve("id_chathub_srv.pub");
        if (!serverPublicKeyPath.toFile().exists() || !serverPrivateKeyPath.toFile().exists()) {
            CryptManager.init(serverPublicKeyPath, serverPrivateKeyPath, password);
        }
        cryptManager = new CryptManager(serverPublicKeyPath, serverPrivateKeyPath, password);

        //Public group key pair
        Path groupPublicKeyPath = dataDir.resolve("id_chathub_publicgroup");
        Path groupPrivateKeyPath = dataDir.resolve("id_chathub_publicgroup.pub");
        if (!groupPublicKeyPath.toFile().exists() || !groupPrivateKeyPath.toFile().exists()) {
            CryptManager.init(groupPublicKeyPath, groupPrivateKeyPath, password);
        }
        CryptManager groupCryptManager = new CryptManager(groupPublicKeyPath, groupPrivateKeyPath, password);
        Group publicGroup = new Group("Public Group", groupCryptManager.publicKey, groupCryptManager.privateKey);
        publicGroup.UUID = UUID.randomUUID().toString();
        this.groups.add(publicGroup);
    }

    /**
     * Starts the server listening on the local network and providing the chat service.
     * The backend service is configured through listeners and callbacks to correctly process
     * requests and responses from/to clients.
     *
     * @see TCPUnifiedService the main class providing the chat service
     * @throws Exception in case of an unexpected and fatal exception, which is supposed
     * to halt the exception of the backend service (all others are handled appropriately)
     */
    public void start() throws Exception {
        /* fetch the server listening port from local configuration */
        String portStr = Configuration.getProp("port");
        int port = Integer.parseInt(portStr);

        ClientDisconnectCallback disconnectCallback = client -> {
            clients.remove(client);
            groups.forEach(group -> {
                // check if the user is in the group
                group.getParticipantsUUIDs().remove(client.getUser().getUUID());
            });
            // send the event to all the clients
            ChatEntityRemovedEvent event = new ChatEntityRemovedEvent();
            event.uuid = client.getUser().getUUID();
            clients.forEach(c -> {
                try {
                    c.sendEvent(event);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            Log.info("Client disconnected: " + client.getUser().getUUID());
        };

        UUIDResolver uuidResolver = uuid -> {
            for (ClientConnection client : clients) {
                if (client.getUser().getUUID().equals(uuid)) {
                    return client;
                }
            }
            return null;
        };

        ServiceServer service = null;
        if (Configuration.getProp("mode").equals("tcp")) {
            Log.info("Starting TCP server on port " + port);
            service = new TCPUnifiedService(port, disconnectCallback);
        } else if (Configuration.getProp("mode").equals("rmi")) {
            Log.info("Starting RMI server on port " + port);
            service = new RMIUnifiedService(port, disconnectCallback, uuidResolver);
        }


        /* request interceptor, responsible for decrypting incoming packets (if encrypted)
           using the server's private key */
        service.addPacketInterceptor(packet -> {
            try {
                if (packet instanceof EncryptedObjectPacket) {
                    EncryptedObjectPacket eop = (EncryptedObjectPacket) packet;
                    return new Packet(cryptManager.decrypt(eop));
                }
            } catch (Exception e) {
                Log.warning("Could not decrypt packet", e);
            }
            return packet;
        });



        /* response interceptor, responsible for finding the connected client by looking up
           the socket in use and encrypting the outgoing packet with its public key */
        service.addResponseInterceptor((clientConnection, request, response) -> {
            try {
                return CryptManager.encrypt(
                        response.getData(), clientConnection.getUser().getPublicKey());
            } catch (Exception e) {
                Log.warning(String.format("Could not encrypted response to %s", clientConnection.getUser().getUUID(), e));
            }
            return null;
        });


        service.addHandler(
                HandshakeRequestEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (clientConnection, payload) -> {
                    HandshakeRequestEvent handshakeRequestEvent = (HandshakeRequestEvent) payload;
                    HandshakeResponseEvent handshakeResponseEvent = new HandshakeResponseEvent();
                    TCPClientConnection newUser;
                    try {
                        clientConnection.getUser().publicKey = handshakeRequestEvent.publicKey;
                        clientConnection.getUser().UUID = UUID.randomUUID().toString();
                        ChatEntityAddedEvent entityAdded = new ChatEntityAddedEvent();
                        entityAdded.entity = clientConnection.getUser();
                        Log.info("New user connected: " + clientConnection.getUser().getUUID());
                        //Broadcast join event
                        this.clients.forEach(client -> {
                            try {
                                client.sendEvent(entityAdded);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        handshakeResponseEvent.uuid = clientConnection.getUser().getUUID();
                        handshakeResponseEvent.serverPublicKey = CryptManager.publicKeyToBytes(cryptManager.getPublicKey());
                        //combine values of users with groups into ChatEntity list
                        handshakeResponseEvent.groups = (LinkedList<Group>) this.groups;
                        handshakeResponseEvent.users = this.clients.stream().map(ClientConnection::getUser).collect(Collectors.toCollection(LinkedList::new));
                        this.clients.add(clientConnection);
                        this.groups.get(0).participantsUUIDs.add(clientConnection.getUser().getUUID()); // Add user to public group
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                    return handshakeResponseEvent;
                }
        );

        service.addHandler(
                SetUsernameEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (clientConnection, payload) -> {
                    SetUsernameEvent event = (SetUsernameEvent) payload;
                    User user = clientConnection.getUser();
                    user.username = event.username;
                    ChangeUsernameEvent changeUsernameEvent = new ChangeUsernameEvent();
                    changeUsernameEvent.uuid = user.getUUID();
                    changeUsernameEvent.username = user.username;
                    this.clients.forEach(client -> {
                        try {
                            if (client.getUser().getUUID() != user.getUUID()) {
                                client.sendEvent(changeUsernameEvent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    Log.info(String.format("User %s set username to %s", user.getUUID(), user.username));
                    return null;
                }
        );

        service.addHandler(
                MessageEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (clientConnection, payload) -> {
                    MessageEvent event = (MessageEvent) payload;
                    //If receiver is user
                    User recUser = this.clients.stream().map(ClientConnection::getUser).filter(s -> s.getUUID().equals(event.receiverUUID)).findFirst().orElse(null);
                    if (recUser != null) {
                        try {
                            this.clients.stream().filter(s -> s.getUser().getUUID().equals(event.receiverUUID)).findFirst().get().sendEvent(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    //If receiver is group
                    Group recGroup = this.groups.stream().filter(s -> s.getUUID().equals(event.receiverUUID)).findFirst().orElse(null);
                    if (recGroup != null) {
                        for (String participantUUID : recGroup.getParticipantsUUIDs()) {
                            ClientConnection participant = this.clients.stream().filter(s -> s.getUser().getUUID().equals(participantUUID)).findFirst().get();
                            if (participant.getUser().getUUID().equals(event.senderUUID)) continue;
                            try {
                                participant.sendEvent(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return null;
                }
        );
    }
}
