package it.eliasandandrea.chathubbackend;

import it.eliasandandrea.chathubbackend.zeroconf.ServiceRegistrator;

public class ChatHubBackend {

    public static void main(String[] args) {
        ServiceRegistrator.registerService();
        while (true){}
    }

}
