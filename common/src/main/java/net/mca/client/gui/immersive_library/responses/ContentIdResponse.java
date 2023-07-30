package net.mca.client.gui.immersive_library.responses;

public record ContentIdResponse(int contentid) implements Response {
    @Override
    public String toString() {
        return "ContentIdResponse{" +
                "contentid=" + contentid +
                '}';
    }
}
