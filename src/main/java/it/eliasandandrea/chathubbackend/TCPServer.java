package it.eliasandandrea.chathubbackend;

import it.eliasandandrea.chathubbackend.encryption.RSACipher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    public TCPServer(int port, RSACipher rsaCipher) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            Socket anotherClient = serverSocket.accept();
            String uuid = UUID.randomUUID().toString();
            while (Client.clients.containsKey(uuid)){
                uuid = UUID.randomUUID().toString();
            }
            System.out.println("New client connected with UUID: " + uuid);
            Client.clients.put(uuid, new Client(anotherClient, uuid));
        }
    }

}
