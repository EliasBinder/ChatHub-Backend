package it.eliasandandrea.chathubbackend;

import it.eliasandandrea.chathubbackend.configUtil.Configuration;
import it.eliasandandrea.chathubbackend.encryption.RSACipher;
import it.eliasandandrea.chathubbackend.zeroconf.ServiceRegistrator;

import java.io.IOException;
import java.nio.file.Path;

public class ChatHubBackend {

    public static void main(String[] args) {
        Configuration.init();
        ServiceRegistrator.registerService();



        RSACipher rsaCipher = null;
        Path appDir = Path.of(System.getProperty("user.home"), ".chathub-server");
        if (!appDir.toFile().exists()) {
            appDir.toFile().mkdirs();
        }
        Path publicKeyPath = appDir.resolve("public.key");
        Path privateKeyPath = appDir.resolve("private.key");
        if (!publicKeyPath.toFile().exists() || !privateKeyPath.toFile().exists()) {
            try {
                RSACipher.init(publicKeyPath, privateKeyPath, Configuration.properties.getProperty("keystorePassword"));
                rsaCipher = new RSACipher(publicKeyPath, privateKeyPath, Configuration.properties.getProperty("keystorePassword"));
            } catch (Exception e) {
                System.err.println("Error while creating the keystore");
                System.exit(1);
            }
        }else {
           try{
               rsaCipher = new RSACipher(publicKeyPath, privateKeyPath, Configuration.properties.getProperty("keystorePassword"));
           }catch (Exception e){
               System.err.println("Error while loading the keystore! Maybe the password is wrong?");
               System.exit(1);
           }
        }



        try {
            new TCPServer(Integer.parseInt(Configuration.properties.getProperty("port")), rsaCipher);
        } catch (IOException e) {
            System.err.println("Could not start the server!");
            e.printStackTrace();
        }
    }

}
