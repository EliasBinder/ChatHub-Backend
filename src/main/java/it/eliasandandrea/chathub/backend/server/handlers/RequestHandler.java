package it.eliasandandrea.chathub.backend.server.handlers;

import it.eliasandandrea.chathub.backend.server.ClientConnection;
import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;
import it.eliasandandrea.chathub.shared.protocol.rmi.MessageCallbackEvent;

public interface RequestHandler<Req extends ClientEvent, Res extends ServerEvent> {
    Res handle(ClientConnection sender, Req payload);
}
