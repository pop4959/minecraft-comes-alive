package net.mca.client.gui.immersiveLibrary;

import net.mca.client.gui.SkinLibraryScreen;
import net.mca.client.gui.immersiveLibrary.types.LiteContent;
import net.mca.client.resources.SkinMeta;
import net.mca.entity.ai.relationship.Gender;
import net.mca.resources.data.skin.Clothing;
import net.mca.resources.data.skin.Hair;
import net.mca.resources.data.skin.SkinListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.util.LinkedList;
import java.util.Queue;

public final class Workspace {
    private static final int MAX_HISTORY = 50;

    public SkinLibraryScreen.SkinType skinType;

    public int contentid = -1;
    public int temperature;
    public double chance = 1.0;
    public String title = "Unnamed Asset";
    public String profession;
    public Gender gender = Gender.NEUTRAL;

    public int fillToolThreshold = 32;

    public final NativeImage currentImage;
    public final NativeImageBackedTexture backendTexture;

    public LinkedList<NativeImage> history = new LinkedList<>();

    private boolean dirty;
    private boolean dirtySinceSnapshot;


    public Workspace(NativeImage image) {
        this.currentImage = image;
        this.backendTexture = new NativeImageBackedTexture(currentImage);
        this.dirty = true;
    }

    public Workspace(NativeImage image, SkinMeta meta, LiteContent content) {
        this(image);

        this.contentid = content.contentid();
        this.title = content.title();

        this.skinType = content.hasTag("clothing") ? SkinLibraryScreen.SkinType.CLOTHING : SkinLibraryScreen.SkinType.HAIR;

        this.chance = meta.getChance();
        this.gender = meta.getGender();
        this.profession = meta.getProfession();
        this.temperature = meta.getTemperature();
    }

    public SkinListEntry toListEntry() {
        if (skinType == SkinLibraryScreen.SkinType.CLOTHING) {
            return new Clothing("immersive_library:" + contentid, profession, temperature, false, gender);
        } else {
            return new Hair("immersive_library:" + contentid);
        }
    }

    private record FillTodo(int x, int y, int red, int green, int blue, int alpha) {

    }

    private void fillDeleteFunc(FillTodo entry, Queue<FillTodo> todo, int x, int y) {
        if (x < 0 || y < 0 || x >= 64 || y >= 64) return;

        FillTodo nextEntry = new FillTodo(x, y, currentImage.getRed(x, y), currentImage.getGreen(x, y), currentImage.getBlue(x, y), currentImage.getOpacity(x, y));

        if (Math.abs(nextEntry.red - entry.red) > fillToolThreshold) return;
        if (Math.abs(nextEntry.green - entry.green) > fillToolThreshold) return;
        if (Math.abs(nextEntry.blue - entry.blue) > fillToolThreshold) return;
        if (Math.abs(nextEntry.alpha - entry.alpha) > fillToolThreshold) return;

        todo.add(nextEntry);
    }

    public void fillDelete(int x, int y) {
        if (x < 0 || y < 0 || x >= 64 || y >= 64) return;

        saveSnapshot(true);

        Queue<FillTodo> todo = new LinkedList<>();
        todo.add(new FillTodo(x, y, currentImage.getRed(x, y), currentImage.getGreen(x, y), currentImage.getBlue(x, y), currentImage.getOpacity(x, y)));

        while (!todo.isEmpty()) {
            FillTodo entry = todo.poll();

            if (currentImage.getOpacity(entry.x, entry.y) == 0) {
                continue;
            }

            currentImage.setColor(entry.x, entry.y, 0);
            dirty = true;

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    if (ox != 0 || oy != 0) {
                        fillDeleteFunc(entry, todo, entry.x + ox, entry.y + oy);
                    }
                }
            }
        }
    }

    public boolean validPixel(int x, int y) {
        return x >= 0 && x < 64 && y >= 0 && y < 64;
    }

    public void saveSnapshot(boolean always) {
        if (always || dirtySinceSnapshot) {
            dirtySinceSnapshot = false;
            while (history.size() > MAX_HISTORY) {
                history.removeFirst().close();
            }
            NativeImage image = new NativeImage(64, 64, false);
            image.copyFrom(currentImage);
            history.add(image);
        }
    }

    public void undo() {
        if (history.size() > 0) {
            NativeImage image = history.removeLast();
            currentImage.copyFrom(image);
            image.close();
            dirty = true;
            dirtySinceSnapshot = false;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            dirtySinceSnapshot = true;
        }
    }
}
