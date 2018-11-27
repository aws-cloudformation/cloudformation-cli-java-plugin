package com.aws.cfn;

public class Response {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Response(final String message) {
        this.message = message;
    }

    public Response() {
    }
}
