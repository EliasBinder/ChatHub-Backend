module  it.eliasandandrea.chathub {
    requires javax.jmdns;
    requires java.sql;
    requires java.rmi;
    requires ChatHub.Shared;
    exports it.eliasandandrea.chathub.backend;
}