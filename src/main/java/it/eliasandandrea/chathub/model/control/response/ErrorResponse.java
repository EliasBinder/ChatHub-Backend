package it.eliasandandrea.chathub.model.control.response;

public class ErrorResponse implements Response {

    private final String exceptionClass;
    private final String exceptionMessage;

    public ErrorResponse(String exceptionClass, String exceptionMessage) {
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
