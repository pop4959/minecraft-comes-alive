package net.mca.client.gui.immersiveLibrary.types;

import java.util.List;

public record User(int userid, String username, int likes_received, List<LiteContent> likes, List<LiteContent> submissions, boolean moderator) {
    @Override
    public String toString() {
        return "User{" +
                "userid=" + userid +
                ", username='" + username + '\'' +
                ", likes_received=" + likes_received +
                ", likes=" + likes +
                ", submissions=" + submissions +
                ", moderator=" + moderator +
                '}';
    }
}
