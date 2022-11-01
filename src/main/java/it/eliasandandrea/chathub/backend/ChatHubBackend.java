package it.eliasandandrea.chathub.backend;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.backend.server.BackendUnifiedService;
import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.backend.server.handlers.RequestHandler;
import it.eliasandandrea.chathub.backend.zeroconf.ServiceRegistrar;
import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.model.ChatEntity;
import it.eliasandandrea.chathub.shared.model.Group;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.SetUsernameEvent;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.ChatEntityAdded;
import it.eliasandandrea.chathub.shared.protocol.serverEvents.HandshakeResponseEvent;
import it.eliasandandrea.chathub.shared.util.LocalPaths;
import it.eliasandandrea.chathub.shared.util.Log;

import java.nio.file.Path;
import java.util.*;

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
        Group publicGroup = new Group("Public Group", new User[0], groupCryptManager.publicKey, groupCryptManager.privateKey);
        groups.add(publicGroup);
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
        final BackendUnifiedService service = new BackendUnifiedService(port);
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
            if (this.clients.stream().map(s -> s.getSocket()).anyMatch(s -> s == socket)) {
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
                    ClientConnection newUser;
                    try {
                        newUser = new ClientConnection("", handshakeRequestEvent.getPublicKey(), socket, din, dos);
                        newUser.getUser().UUID = UUID.randomUUID().toString();
                        ChatEntityAdded entityAdded = new ChatEntityAdded();
                        entityAdded.entity = newUser.getUser();
                        //Broadcast join event
                        this.clients.forEach(client -> {
                            try {
                                client.sendEvent(entityAdded);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        this.clients.add(newUser);
                        this.groups.get(0).addUser(newUser.getUser()); // Add user to public group
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                    HandshakeResponseEvent handshakeResponseEvent = new HandshakeResponseEvent();
                    handshakeResponseEvent.uuid = newUser.getUser().getUUID();
                    handshakeResponseEvent.serverPublicKey = CryptManager.publicKeyToBytes(cryptManager.getPublicKey());
                    //combine values of users with groups into ChatEntity list
                    List<ChatEntity> chatEntities = new LinkedList<>();
                    chatEntities.addAll(this.groups);
                    chatEntities.addAll(this.clients.stream().map(c -> c.getUser()).toList());
                    handshakeResponseEvent.chats = chatEntities.toArray(new ChatEntity[0]);
                    return handshakeResponseEvent;
                }
        );

        service.addHandler(
                SetUsernameEvent.class,
                (RequestHandler<ClientEvent, ServerEvent>) (socket, din, dos, payload) -> {
                    SetUsernameEvent event = (SetUsernameEvent) payload;
                    User user = this.clients.stream().filter(s -> s.getSocket() == socket).findFirst().get().getUser();
                    user.username = event.username;
                    Log.info(String.format("User %s set username to %s", user.getUUID(), user.username));
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
