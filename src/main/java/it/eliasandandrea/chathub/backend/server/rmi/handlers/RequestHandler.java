package it.eliasandandrea.chathub.backend.server.tcp.handlers;

import it.eliasandandrea.chathub.shared.protocol.ClientEvent;
import it.eliasandandrea.chathub.shared.protocol.ServerEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public interface RequestHandler<Req extends ClientEvent, Res extends ServerEvent> {
    Res handle(Socket socket, DataInputStream din, DataOutputStream dout, Req payload);
}
