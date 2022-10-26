package it.eliasandandrea.chathubbackend.zeroconf;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceRegistrator {

    public static void registerService() {
        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            // Register a service
            ServiceInfo serviceInfo = ServiceInfo.create("_chathub._tcp.local.", "Sample Server", 5476, "");
            jmdns.registerService(serviceInfo);
            System.out.println("[Zeroconf] Service registered");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                jmdns.unregisterAllServices();
                System.out.println("Service unregistered");
                try {
                    jmdns.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
