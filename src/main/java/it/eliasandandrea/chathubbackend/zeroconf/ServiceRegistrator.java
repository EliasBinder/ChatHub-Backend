package it.eliasandandrea.chathubbackend.zeroconf;

import it.eliasandandrea.chathubbackend.configUtil.Configuration;

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
            ServiceInfo serviceInfo = ServiceInfo.create("_chathub._tcp.local.", Configuration.properties.getProperty("name"), Integer.parseInt(Configuration.properties.getProperty("port")), "");
            jmdns.registerService(serviceInfo);
            System.out.println("[Zeroconf] Service registered");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                jmdns.unregisterAllServices();
                System.out.println("[Zeroconf] Service unregistered");
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
