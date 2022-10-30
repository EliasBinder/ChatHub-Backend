package it.eliasandandrea.chathubbackend.server.handlers;

import it.eliasandandrea.chathub.model.control.request.Request;
import it.eliasandandrea.chathub.model.control.response.Response;

import java.net.Socket;

public interface RequestHandler<Req extends Request, Res extends Response> {
    Res handle(Socket socket, Req payload);
}
