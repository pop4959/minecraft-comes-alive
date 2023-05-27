package net.mca.client.gui.immersiveLibrary.responses;

import java.util.Arrays;

public record TagListResponse(String[] tags) implements Response {
    @Override
    public String toString() {
        return "TagListResponse{" +
                "tags=" + Arrays.toString(tags) +
                '}';
    }
}
