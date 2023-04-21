package net.mca.client.gui.immersiveLibrary.responses;

public record IsAuthResponse(boolean authenticated) implements Response {
    @Override
    public String toString() {
        return "IsAuthResponse{" +
                "authenticated=" + authenticated +
                '}';
    }
}
