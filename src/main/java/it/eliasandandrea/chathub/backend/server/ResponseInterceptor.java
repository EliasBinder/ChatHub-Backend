package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.crypto.Packet;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;

public interface ResponseInterceptor {

    Packet intercept(ClientConnection sender, ClientEvent request, Packet response);

}
