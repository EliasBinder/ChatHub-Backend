package it.eliasandandrea.chathubbackend.encryption;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

public class Keystore {

    //singleton
    private static Keystore singleton;
    public static Keystore getInstance(){
        if (singleton == null)
            singleton = new Keystore();
        return singleton;
    }

    private HashMap<String, KeyCombination> keysMap;

    public Keystore(){
        this.keysMap = new HashMap<>();
    }

    public void addKey(String user, PublicKey publicKey){
        keysMap.put(user, new KeyCombination(publicKey, null));
    }

    public void addKey(String group, PublicKey publicKey, PrivateKey privateKey){
        keysMap.put(group, new KeyCombination(publicKey, privateKey));
    }

    public PublicKey getPublicKey(String thing){
        return keysMap.get(thing).publicKey;
    }

    public PrivateKey getPrivateKey(String thing){
        return keysMap.get(thing).privateKey;
    }

}
