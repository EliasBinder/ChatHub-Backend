package it.eliasandandrea.chathub.backend;

import it.eliasandandrea.chathub.backend.configUtil.Configuration;
import it.eliasandandrea.chathub.backend.zeroconf.ServiceRegistrar;

public class ChatHubBackend {

    public static void main(String[] args) throws Exception {
        Configuration.init();
        ServiceRegistrar.registerServices();
        new ServerImpl(
                Configuration.properties.getProperty("keystorePassword")
        ).start();
    }
}
