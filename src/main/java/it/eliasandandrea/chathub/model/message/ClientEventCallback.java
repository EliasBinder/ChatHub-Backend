package it.eliasandandrea.chathub.model.message;

import it.eliasandandrea.chathubbackend.Client;

@FunctionalInterface
public interface ClientEventCallback {
    void onEvent(ClientEvent event, Client sender);
}
