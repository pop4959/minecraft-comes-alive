package net.mca.client.gui.immersiveLibrary.types;

import java.util.Arrays;

public record LiteContent(int contentid, String username, int likes, String[] tags, String title, int version) implements Tagged {
    @Override
    public String toString() {
        return "Content{" +
                "contentid=" + contentid +
                ", username='" + username + '\'' +
                ", likes=" + likes +
                ", tags=" + Arrays.toString(tags) +
                ", title='" + title + '\'' +
                ", version=" + version +
                '}';
    }
}
