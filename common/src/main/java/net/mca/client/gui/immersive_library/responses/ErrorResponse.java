package net.mca.client.gui.immersive_library.responses;

public record ErrorResponse(int code, String message) implements Response {
    @Override
    public String toString() {
        return "ErrorResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
