package it.eliasandandrea.chathub.backend.server;

import it.eliasandandrea.chathub.shared.crypto.Packet;

public interface PacketInterceptor {

    Packet intercept(Packet packet);

}
