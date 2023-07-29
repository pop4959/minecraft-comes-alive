package net.mca.client.gui.immersive_library.responses;

import net.mca.client.gui.immersive_library.types.LiteContent;

import java.util.Arrays;

public record ContentListResponse(LiteContent[] contents) implements Response {
    @Override
    public String toString() {
        return "ContentListResponse{" +
                "contents=" + Arrays.toString(contents) +
                '}';
    }
}
