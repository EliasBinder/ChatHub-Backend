package it.eliasandandrea.chathub.model.control.request;

public abstract class AuthenticatedRequest implements Request {

    private String uuid;

    public AuthenticatedRequest(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
