package it.eliasandandrea.chathub.backend;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.backend.server.BackendUnifiedService;
import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.backend.zeroconf.ServiceRegistrar;
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

import java.net.Socket;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ChatHubBackend {

    private final List<ClientConnection> clients;
    private final List<Group> groups;
    private CryptManager cryptManager;

    public ChatHubBackend(String password) throws Exception {
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

    public static void main(String[] args) throws Exception {
        Configuration.init();
        ServiceRegistrar.registerServices();

//        final Scanner passwordScanner = new Scanner(System.in);
//        System.out.println("Passphrase: ");
//        String password = passwordScanner.next();
//        new ChatHubBackend(LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv"),
//                LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv.pub"), password).start();

        new ChatHubBackend(
                Configuration.properties.getProperty("keystorePassword")
        ).start();
    }

    public void start() throws Exception {
        String portStr = Configuration.getProp("port");
        int port = Integer.parseInt(portStr);
        final BackendUnifiedService service = new BackendUnifiedService(port){
            @Override
            public void onException(Exception e, Socket socket) {
                if (socket != null) {
                    respondWithError(socket, e);
                }
                ClientConnection clientConnection =  clients.stream().filter(s -> s.getSocket() == socket).findFirst().orElse(null);
                if (clientConnection != null){
                    clients.remove(clientConnection);
                    groups.forEach(group -> {
                        //check if the user is in the group
                        group.getParticipantsUUIDs().remove(clientConnection.getUser().getUUID());
                    });
                    //send the event to all the clients
                    ChatEntityRemovedEvent event = new ChatEntityRemovedEvent();
                    event.uuid = clientConnection.getUser().getUUID();
                    clients.forEach(c -> {
                        try {
                            c.sendEvent(event);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    });
                }
                Log.warning("exception in BackendUnifiedService", e);
            }
        };
        service.addPacketInterceptor(packet -> {
            try {
                if (packet instanceof EncryptedObjectPacket eop) {
                    return new Packet(cryptManager.decrypt(eop));
                }
            } catch (Exception e) {
                Log.warning("Could not decrypt packet", e);
            }
            return packet;
        });
        service.addResponseInterceptor((socket, request, response) -> {
            if (this.clients.stream().map(ClientConnection::getSocket).anyMatch(s -> s == socket)) {
                try {
                    return CryptManager.encrypt(
                            response.getData(), this.clients.stream().filter(s -> s.getSocket() == socket).findFirst().get().getUser().getPublicKey());
                } catch (Exception e) {
                    Log.warning(String.format("Could not encrypted response to %s", this.clients.stream().filter(s -> s.getSocket() == socket).findFirst().get().getUser().getUUID()), e);
                }
            }
            return null;
        });


        service.addHandler(
                HandshakeRequestEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (socket, din, dos, payload) -> {
                    HandshakeRequestEvent handshakeRequestEvent = (HandshakeRequestEvent) payload;
                    HandshakeResponseEvent handshakeResponseEvent = new HandshakeResponseEvent();
                    ClientConnection newUser;
                    try {
                        newUser = new ClientConnection("", handshakeRequestEvent.getPublicKey(), socket, din, dos);
                        newUser.getUser().UUID = UUID.randomUUID().toString();
                        ChatEntityAddedEvent entityAdded = new ChatEntityAddedEvent();
                        entityAdded.entity = newUser.getUser();
                        System.out.println("New user connected: " + newUser.getUser().getUUID());
                        //Broadcast join event
                        this.clients.forEach(client -> {
                            try {
                                client.sendEvent(entityAdded);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        handshakeResponseEvent.uuid = newUser.getUser().getUUID();
                        handshakeResponseEvent.serverPublicKey = CryptManager.publicKeyToBytes(cryptManager.getPublicKey());
                        //combine values of users with groups into ChatEntity list
                        handshakeResponseEvent.groups = (LinkedList<Group>) this.groups;
                        handshakeResponseEvent.users = this.clients.stream().map(ClientConnection::getUser).collect(Collectors.toCollection(LinkedList::new));
                        this.clients.add(newUser);
                        this.groups.get(0).participantsUUIDs.add(newUser.getUser().getUUID()); // Add user to public group
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                    return handshakeResponseEvent;
                }
        );

        service.addHandler(
                SetUsernameEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (socket, din, dos, payload) -> {
                    SetUsernameEvent event = (SetUsernameEvent) payload;
                    User user = this.clients.stream().filter(s -> s.getSocket() == socket).findFirst().get().getUser();
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
                (RequestHandler<ClientEvent, ServerEvent>) (socket, din, dos, payload) -> {
                    MessageEvent event = (MessageEvent) payload;
                    //If receiver is user
                    User recUser = this.clients.stream().map(ClientConnection::getUser).filter(s -> s.getUUID().equals(event.receiverUUID)).findFirst().orElse(null);
                    if (recUser != null){
                        try {
                            this.clients.stream().filter(s -> s.getUser().getUUID().equals(event.receiverUUID)).findFirst().get().sendEvent(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    //If receiver is group
                    Group recGroup = this.groups.stream().filter(s -> s.getUUID().equals(event.receiverUUID)).findFirst().orElse(null);
                    if (recGroup != null){
                        for (String participantUUID : recGroup.getParticipantsUUIDs()) {
                            ClientConnection clientConnection = this.clients.stream().filter(s -> s.getUser().getUUID().equals(participantUUID)).findFirst().get();
                            if (clientConnection.getUser().getUUID().equals(event.senderUUID)) continue;
                            try {
                                clientConnection.sendEvent(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return null;
                }
        );

        boolean run = true;
        while (run) {
            try {
                service.waitForConnection();
            } catch (Exception e) {
                e.printStackTrace();
                run = false;
            }
        }
    }
}
