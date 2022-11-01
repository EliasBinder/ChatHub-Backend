package it.eliasandandrea.chathub.backend.server.handlers;

import it.eliasandandrea.chathub.shared.model.User;

import java.net.Socket;

public interface NewUserCallback {

    void onNewUser(Socket socket, User user);


}
