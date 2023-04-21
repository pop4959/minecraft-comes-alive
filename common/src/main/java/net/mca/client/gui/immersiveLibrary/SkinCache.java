package net.mca.client.gui.immersiveLibrary;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.mca.client.gui.immersiveLibrary.responses.ContentResponse;
import net.mca.client.gui.immersiveLibrary.responses.Response;
import net.mca.client.gui.immersiveLibrary.types.Content;
import net.mca.client.resources.SkinMeta;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.mca.client.gui.immersiveLibrary.Api.request;

public class SkinCache {
    private static final Identifier DEFAULT_SKIN = new Identifier("textures/entity/steve.png");

    private static final Gson gson = new Gson();

    static Set<Integer> requested = new HashSet<>();
    static Map<Integer, Identifier> textureIdentifierCache = new HashMap<>();
    static Map<Integer, SkinMeta> metaCache = new HashMap<>();

    private static File getFile(String key) {
        //noinspection ResultOfMethodCallIgnored
        new File("./immersive_library_mca/").mkdirs();

        return new File("./immersive_library_mca/" + key);
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

    public static void clearRequested() {
        requested.clear();
    }

    public static void enforceSync(Content content) {
        //noinspection ResultOfMethodCallIgnored
        getFile(content.contentid() + ".version").delete();
    }

    public static void sync(Content content) {
        if (!requested.contains(content.contentid())) {
            requested.add(content.contentid());

            int version = -1;
            try {
                String s = FileUtils.readFileToString(getFile(content.contentid() + ".version"), Charset.defaultCharset());
                version = Integer.parseInt(s);
            } catch (Exception ignored) {
            }

            if (content.version() == version) {
                loadResources(content);
            } else {
                //noinspection ResultOfMethodCallIgnored
                getFile(content.contentid() + ".version").delete();

                CompletableFuture.runAsync(() -> {
                    Response response = request(Api.HttpMethod.GET, ContentResponse.class, "content/mca/%s".formatted(content.contentid()));
                    if (response instanceof ContentResponse contentResponse) {
                        write(content.contentid() + ".png", Base64.getDecoder().decode(contentResponse.content().data()));
                        write(content.contentid() + ".json", contentResponse.content().meta());
                        write(content.contentid() + ".version", Integer.toString(contentResponse.content().version()));
                        loadResources(content);
                    }
                });
            }
        }
    }

    private static void loadResources(Content content) {
        // Load meta
        try {
            String json = read(content.contentid() + ".json");
            SkinMeta meta = gson.fromJson(json, SkinMeta.class);
            metaCache.put(content.contentid(), meta);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            enforceSync(content);
        }

        // Load texture
        try (FileInputStream stream = new FileInputStream(getFile(content.contentid() + ".png").getPath())) {
            // Load new
            NativeImage image = NativeImage.read(stream);
            Identifier identifier = new Identifier("immersive_library_mca", String.valueOf(content.contentid()));
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(image));
            textureIdentifierCache.put(content.contentid(), identifier);
        } catch (IOException e) {
            e.printStackTrace();
            enforceSync(content);
        }
    }

    @Nullable
    public static SkinMeta getMeta(Content content) {
        sync(content);
        return metaCache.get(content.contentid());
    }

    public static Identifier getTextureIdentifier(Content content) {
        sync(content);
        return textureIdentifierCache.getOrDefault(content.contentid(), DEFAULT_SKIN);
    }
}
