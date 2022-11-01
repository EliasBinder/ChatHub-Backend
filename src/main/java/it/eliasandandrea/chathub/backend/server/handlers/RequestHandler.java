package it.eliasandandrea.chathub.backend.server.handlers;

import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;

import java.net.Socket;

public interface RequestHandler<Req extends ClientEvent, Res extends ServerEvent> {
    Res handle(Socket socket, Req payload);
}
