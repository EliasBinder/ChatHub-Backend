package it.eliasandandrea.chathub.backend.zeroconf;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.shared.util.Log;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceRegistrar {

    public static void registerServices() {
        // Create an auto-closing JmDNS instance
        try (final JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost())) {
            String portStr = Configuration.getProp("port");
            int port = Integer.parseInt(portStr);
            // Register the control service and the chat service
            ServiceInfo chatServiceInfo = ServiceInfo.create("_chathub._tcp.local.",
                    Configuration.getProp("name"), port, "type=" + getType());
            jmdns.registerService(chatServiceInfo);

            Log.info("[Zeroconf] Services registered");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                jmdns.unregisterAllServices();
                Log.info("Services unregistered");
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

    private static int getType(){
        String mode = Configuration.getProp("mode");
        if (mode.equals("tcp")){
            return 1;
        }else if (mode.equals("rmi")){
            return 2;
        }
        return 1;
    }

}
