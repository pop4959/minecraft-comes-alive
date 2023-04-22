package net.mca.client.gui.immersiveLibrary.types;

import java.util.Arrays;

public record LiteContent(int contentid, int userid, String username, int likes, String[] tags, String title, int version) implements Tagged {
    @Override
    public String toString() {
        return "Content{" +
                "contentid=" + contentid +
                "userid=" + userid +
                ", username='" + username + '\'' +
                ", likes=" + likes +
                ", tags=" + Arrays.toString(tags) +
                ", title='" + title + '\'' +
                ", version=" + version +
                '}';
    }
}
