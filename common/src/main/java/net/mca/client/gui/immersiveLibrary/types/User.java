package net.mca.client.gui.immersiveLibrary.types;

public record User(int userid, String username, int likesReceived, LiteContent likedContent, LiteContent submissions, boolean moderator) {
    @Override
    public String toString() {
        return "User{" +
                "userid=" + userid +
                ", username='" + username + '\'' +
                ", likesReceived=" + likesReceived +
                ", likedContent=" + likedContent +
                ", submissions=" + submissions +
                ", moderator=" + moderator +
                '}';
    }
}
