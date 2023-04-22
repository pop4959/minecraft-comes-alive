package net.mca.client.gui.immersiveLibrary.types;

import java.util.Arrays;

public record Content(int contentid, int userid, String username, int likes, String[] tags, String title, int version, String meta, String data) implements Tagged {
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
                ", meta='" + meta + '\'' +
                ", data='" + data + '\'' +
                '}';
    }

    public boolean hasTag(String filter) {
        for (String tag : tags) {
            if (tag.equals(filter)) {
                return true;
            }
        }
        return false;
    }
}
