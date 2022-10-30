package it.eliasandandrea.chathubbackend.zeroconf;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceRegistrar {

    public static void registerServices() {
        // Create an auto-closing JmDNS instance
        try (final JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost())) {
            // Register the control service and the chat service
            ServiceInfo chatServiceInfo = ServiceInfo.create("_chathub._tcp.local.",
                    "ChatHub Chat Server", 5476, "ip=" + InetAddress.getLocalHost().getHostAddress());
            jmdns.registerService(chatServiceInfo);

            System.out.println("[Zeroconf] Services registered");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                jmdns.unregisterAllServices();
                System.out.println("Services unregistered");
                try {
                    jmdns.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
