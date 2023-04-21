package net.mca.client.gui.immersiveLibrary.types;

public interface Tagged {
    String[] tags();

    default boolean hasTag(String filter) {
        for (String tag : tags()) {
            if (tag.equals(filter)) {
                return true;
            }
        }
        return false;
    }
}
