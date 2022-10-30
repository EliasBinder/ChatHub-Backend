package it.eliasandandrea.chathubbackend;

import it.eliasandandrea.chathub.model.control.ConnectedClient;
import it.eliasandandrea.chathub.model.control.request.AuthenticatedRequest;
import it.eliasandandrea.chathub.model.control.request.JoinServerRequest;
import it.eliasandandrea.chathub.model.control.response.JoinServerResponse;
import it.eliasandandrea.chathub.model.crypto.CryptManager;
import it.eliasandandrea.chathub.model.crypto.EncryptedObjectPacket;
import it.eliasandandrea.chathub.model.crypto.Packet;
import it.eliasandandrea.chathubbackend.server.BackendUnifiedService;
import it.eliasandandrea.chathubbackend.server.handlers.JoinServerHandler;
import it.eliasandandrea.chathubbackend.util.LocalPaths;
import it.eliasandandrea.chathubbackend.util.Log;
import it.eliasandandrea.chathubbackend.zeroconf.ServiceRegistrar;

import java.nio.file.Path;
import java.util.*;

public class ChatHubBackend {

    private final Map<String, ConnectedClient> clients;

    public ChatHubBackend(Path pubPath, Path privPath, String password) throws Exception {
        this.clients = new HashMap<>();
        CryptManager.init(pubPath, privPath, password);
    }

    public static void main(String[] args) throws Exception {
        ServiceRegistrar.registerServices();

//        final Scanner passwordScanner = new Scanner(System.in);
//        System.out.println("Passphrase: ");
//        String password = passwordScanner.next();
//        new ChatHubBackend(LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv"),
//                LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv.pub"), password).start();

        new ChatHubBackend(LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv"),
                LocalPaths.concat(LocalPaths.getData(), "id_chathub_srv.pub"), "test").start();
    }

    public void start() throws Exception {
        final BackendUnifiedService service = new BackendUnifiedService(5476);
        service.addPacketInterceptor(packet -> {
            try {
                if (packet instanceof EncryptedObjectPacket eop) {
                    return new Packet(CryptManager.getInstance().decrypt(eop));
                }
            } catch (Exception e) {
                Log.warning("Could not decrypt packet", e);
            }
            return packet;
        });
        service.addResponseInterceptor((request, response) -> {
            if (request instanceof AuthenticatedRequest req) {
                String uuid = req.getUuid();
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
        service.addHandler(JoinServerRequest.class, new JoinServerHandler(
                CryptManager.getInstance().getPublicKey(), (client, username, publicKey) -> {
            String uuid = UUID.randomUUID().toString();
            clients.put(uuid, new ConnectedClient(client, username, uuid, publicKey));
            return uuid;
        }));

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
