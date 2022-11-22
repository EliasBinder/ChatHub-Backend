package it.eliasandandrea.chathub.backend.server;

public interface UUIDResolver {

    ClientConnection resolve(String uuid);

}
