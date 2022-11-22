package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.model.User;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;

public abstract class ClientConnection {

    protected User user;

    public ClientConnection(User user) {
        if (user != null)
            this.user = user;
        else{
            //implement dummy user to avoid endless null checks
            this.user = new User(null, null);
            this.user.UUID = "null";
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public abstract void sendEvent(ServerEvent event) throws Exception;
}
