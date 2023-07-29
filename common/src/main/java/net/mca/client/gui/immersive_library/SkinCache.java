package net.mca.client.gui.immersive_library;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.mca.MCA;
import net.mca.client.gui.immersive_library.responses.ContentResponse;
import net.mca.client.gui.immersive_library.responses.Response;
import net.mca.client.gui.immersive_library.types.LiteContent;
import net.mca.client.resources.SkinMeta;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.mca.client.gui.immersive_library.Api.request;

public class SkinCache {
    private static final Identifier DEFAULT_SKIN = MCA.locate("skins/empty.png");

    private static final Gson gson = new Gson();

    static final Map<Integer, Boolean> requested = new ConcurrentHashMap<>();
    static final Map<Integer, Boolean> upToDate = new ConcurrentHashMap<>();

    static final Map<Integer, Identifier> textureIdentifiers = new HashMap<>();
    static final Map<Integer, NativeImage> images = new HashMap<>();
    static final Map<Integer, SkinMeta> metas = new HashMap<>();

    private static File getFile(String key) {
        //noinspection ResultOfMethodCallIgnored
        new File("./immersive_library/").mkdirs();

        return new File("./immersive_library/" + key);
    }

    private static void write(String file, String content) {
        try {
            FileUtils.writeStringToFile(getFile(file), content, Charset.defaultCharset(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(String file, byte[] content) {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(getFile(file)))) {
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String read(String file) {
        try {
            return FileUtils.readFileToString(getFile(file), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param contentid The content id
     * Enforces re downloading the assets, mostly when local files appear to be corrupted
     */
    public static void enforceSync(int contentid) {
        upToDate.remove(contentid);
        try {
            Files.delete(getFile(contentid + ".version").toPath());
        } catch (IOException e) {
            MCA.LOGGER.warn(e);
        }
    }

    public static void sync(LiteContent content) {
        sync(content.contentid(), content.version());
    }

    /**
     * @param contentid      The content id
     * @param currentVersion The current version, used to invalidate the cache
     *                       Downloads the assets if they are not up to date
     */
    public static void sync(int contentid, int currentVersion) {
        if (!requested.containsKey(contentid)) {
            requested.put(contentid, true);

            // Load the current version
            int version = -1;
            File file = getFile(contentid + ".version");
            if (file.exists()) {
                try {
                    String s = FileUtils.readFileToString(file, Charset.defaultCharset());
                    version = Integer.parseInt(s);
                } catch (Exception e) {
                    MCA.LOGGER.warn(e);
                }
            }

            // Download assets when versions mismatch
            if (currentVersion == version) {
                upToDate.put(contentid, true);
                loadResources(contentid);
            } else if (currentVersion >= 0 || !upToDate.containsKey(contentid)) {
                try {
                    Files.delete(getFile(contentid + ".version").toPath());
                } catch (IOException e) {
                    MCA.LOGGER.warn(e);
                }

                CompletableFuture.runAsync(() -> {
                    Response response = request(Api.HttpMethod.GET, ContentResponse.class, "content/mca/%s".formatted(contentid));
                    if (response instanceof ContentResponse contentResponse) {
                        write(contentid + ".png", Base64.getDecoder().decode(contentResponse.content().data()));
                        write(contentid + ".json", contentResponse.content().meta());
                        write(contentid + ".version", Integer.toString(contentResponse.content().version()));
                        upToDate.put(contentid, true);
                        requested.remove(contentid);
                    }
                });
            }
        }
    }

    /**
     * @param contentid The content id
     *                  Loads the resources from the disk and creates the texture identifier
     */
    private static void loadResources(int contentid) {
        // Load meta
        try {
            String json = read(contentid + ".json");
            SkinMeta meta = gson.fromJson(json, SkinMeta.class);
            metas.put(contentid, meta);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            enforceSync(contentid);
        }

        // Load texture
        try (FileInputStream stream = new FileInputStream(getFile(contentid + ".png").getPath())) {
            // Load new
            NativeImage image = NativeImage.read(stream);
            Identifier identifier = new Identifier("immersive_library", String.valueOf(contentid));
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(image));
            textureIdentifiers.put(contentid, identifier);
            images.put(contentid, image);
        } catch (IOException e) {
            e.printStackTrace();
            enforceSync(contentid);
        }
    }

    public static Optional<SkinMeta> getMeta(LiteContent content) {
        sync(content);
        return Optional.ofNullable(metas.get(content.contentid()));
    }

    public static Optional<NativeImage> getImage(LiteContent content) {
        sync(content);
        return Optional.ofNullable(images.get(content.contentid()));
    }

    public static Identifier getTextureIdentifier(LiteContent content) {
        sync(content);
        return textureIdentifiers.getOrDefault(content.contentid(), DEFAULT_SKIN);
    }

    /**
     * @param contentid The content id
     * @return The texture identifier
     * Unlike the other getters this function will sync at least once no matter the local state of the cache, as it lacks the current version
     */
    public static Identifier getTextureIdentifier(int contentid) {
        sync(contentid, -1);
        return textureIdentifiers.getOrDefault(contentid, DEFAULT_SKIN);
    }
}
