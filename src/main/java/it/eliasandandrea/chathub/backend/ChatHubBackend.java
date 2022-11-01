package it.eliasandandrea.chathub.backend;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.backend.server.BackendUnifiedService;
import it.eliasandandrea.chathub.backend.server.handlers.HandshakeHandler;
import it.eliasandandrea.chathub.backend.zeroconf.ServiceRegistrar;
import it.eliasandandrea.chathub.shared.crypto.CryptManager;
import it.eliasandandrea.chathub.shared.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.clientEvents.HandshakeRequestEvent;
import it.eliasandandrea.chathub.shared.util.LocalPaths;
import it.eliasandandrea.chathub.shared.util.Log;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ChatHubBackend {

    private final Map<String, User> clients;
    private CryptManager cryptManager;

    //








    // USE buildOnUNIX.sh in root dir to ensure pulling and building the submodule at least once in order to use it for the build of the backend or client application








    //

    public ChatHubBackend(Path pubPath, Path privPath, String password) throws Exception {
        this.clients = new HashMap<>();
        Path dataDir = LocalPaths.getData();
        Path publicKeyPath = dataDir.resolve("public.key");
        Path privateKeyPath = dataDir.resolve("private.key");
        if (!publicKeyPath.toFile().exists() || !privateKeyPath.toFile().exists()) {
            CryptManager.init(pubPath, privPath, password);
        }
        cryptManager = new CryptManager(pubPath, privPath, password);
    }

    public static void main(String[] args) throws Exception {
        ServiceRegistrar.registerServices();

//        final Scanner passwordScanner = new Scanner(System.in);
//        System.out.println("Passphrase: ");
//        String password = passwordScanner.next();
//        new ChatHubBackend(LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv"),
//                LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv.pub"), password).start();

        new ChatHubBackend(
                LocalPaths.getData().resolve("id_chathub_srv"),
                LocalPaths.getData().resolve("id_chathub_srv.pub"),
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
        service.addResponseInterceptor((request, response) -> {
            if (request instanceof HandshakeRequestEvent req) {
                if (this.clients.containsKey(uuid)) {
                    try {
                        return CryptManager.encrypt(
                                response.getData(), this.clients.get(uuid).getPublicKey());
                    } catch (Exception e) {
                        Log.warning(String.format("Could not encrypted response to %s", uuid), e);
                    }
                }
            }
            return null;
        });
        service.addHandler(
                HandshakeRequestEvent.class,
                new HandshakeHandler(cryptManager.getPublicKey())
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
