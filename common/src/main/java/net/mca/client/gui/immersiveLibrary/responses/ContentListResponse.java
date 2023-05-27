package net.mca.client.gui.immersiveLibrary.responses;

import net.mca.client.gui.immersiveLibrary.types.LiteContent;

import java.util.Arrays;

public record ContentListResponse(LiteContent[] contents) implements Response {
    @Override
    public String toString() {
        return "ContentListResponse{" +
                "contents=" + Arrays.toString(contents) +
                '}';
    }
}
