package net.mca.client.gui.immersiveLibrary.responses;

public record ContentIdResponse(int contentid) implements Response {
    @Override
    public String toString() {
        return "ContentIdResponse{" +
                "contentid=" + contentid +
                '}';
    }
}
