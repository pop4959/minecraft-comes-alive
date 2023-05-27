package net.mca.client.gui.immersiveLibrary.responses;

import net.mca.client.gui.immersiveLibrary.types.Content;

public record ContentResponse(Content content) implements Response {
    @Override
    public String toString() {
        return "ContentResponse{" +
                "content=" + content +
                '}';
    }
}
