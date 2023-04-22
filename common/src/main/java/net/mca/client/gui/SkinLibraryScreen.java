package net.mca.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mca.Config;
import net.mca.MCA;
import net.mca.client.gui.immersiveLibrary.Api;
import net.mca.client.gui.immersiveLibrary.Auth;
import net.mca.client.gui.immersiveLibrary.SkinCache;
import net.mca.client.gui.immersiveLibrary.responses.*;
import net.mca.client.gui.immersiveLibrary.types.LiteContent;
import net.mca.client.gui.immersiveLibrary.types.User;
import net.mca.client.gui.widget.*;
import net.mca.client.resources.ClientUtils;
import net.mca.client.resources.ProfessionIcons;
import net.mca.client.resources.SkinMeta;
import net.mca.cobalt.network.NetworkHandler;
import net.mca.entity.EntitiesMCA;
import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.relationship.Gender;
import net.mca.network.c2s.AddCustomClothingMessage;
import net.mca.network.c2s.RemoveCustomClothingMessage;
import net.mca.resources.data.skin.Clothing;
import net.mca.resources.data.skin.Hair;
import net.mca.resources.data.skin.SkinListEntry;
import net.mca.util.localization.FlowingText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.lwjgl.glfw.GLFW;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.mca.client.gui.immersiveLibrary.Api.request;

public class SkinLibraryScreen extends Screen implements SkinListUpdateListener {
    private static final Identifier EMPTY_IDENTIFIER = MCA.locate("skins/empty.png");
    private static final Identifier CANVAS_IDENTIFIER = MCA.locate("temp");
    private static final float CANVAS_SCALE = 2.35f;

    private String filteredString = "";

    private LinkedHashMap<String, Long> tags = new LinkedHashMap<>();
    private final Set<String> selectedTags = new HashSet<>();

    private final List<LiteContent> content = new ArrayList<>();
    private final List<LiteContent> likedContent = new ArrayList<>();
    private final List<LiteContent> serverContent = new ArrayList<>();
    private List<LiteContent> filteredContent = new ArrayList<>();
    private SubscriptionFilter subscriptionFilter = SubscriptionFilter.LIBRARY;
    private User currentUser;

    private int selectionPage;
    private LiteContent focusedContent;
    private LiteContent hoveredContent;
    private Page page;

    private ButtonWidget pageWidget;

    private NativeImage currentImage;
    private NativeImageBackedTexture backendTexture;
    private WorkspaceSettings settings;
    private boolean shouldUpload = true;

    private final ColorSelector color = new ColorSelector();
    private int activeMouseButton;
    private int lastPixelMouseX;
    private int lastPixelMouseY;

    private float x0, x1, y0, y1;
    private boolean isPanning;
    private double lastMouseX;
    private double lastMouseY;

    private Text error;

    private final VillagerEditorScreen previousScreen;

    protected final VillagerEntityMCA villagerVisualization = Objects.requireNonNull(EntitiesMCA.MALE_VILLAGER.get().create(MinecraftClient.getInstance().world));

    int CLOTHES_H = 10;
    int CLOTHES_V = 2;
    int CLOTHES_PER_PAGE = CLOTHES_H * CLOTHES_V;

    private boolean authenticated = false;
    private boolean awaitingAuthentication = false;
    private boolean isBrowserOpen = false;
    private boolean uploading = false;
    private Thread thread;

    private static HorizontalColorPickerWidget hueWidget;
    private static HorizontalColorPickerWidget saturationWidget;
    private static HorizontalColorPickerWidget brightnessWidget;

    public SkinLibraryScreen() {
        this(null);
    }

    public SkinLibraryScreen(VillagerEditorScreen screen) {
        super(Text.translatable("gui.skin_library.title"));

        this.previousScreen = screen;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (previousScreen == null) {
            super.close();
        } else {
            MinecraftClient.getInstance().setScreen(previousScreen);
        }
    }

    @Override
    protected void init() {
        super.init();

        SkinCache.clearRequested();

        refreshServerContent();

        thread = Thread.currentThread();

        if (page == null) {
            if (Auth.loadToken() == null) {
                setPage(Page.LOADING);
            } else {
                setPage(Page.LOGIN);
            }
            reloadDatabase();
        } else {
            refreshPage();
        }
    }

    private void reloadDatabase() {
        reloadDatabase(() -> {
            if (page == Page.LOADING) {
                setPage(Page.CLOTHING);
            }
        });
    }

    private void reloadDatabase(Runnable callback) {
        CompletableFuture.runAsync(() -> {
            // fetch user
            if (Auth.getToken() != null && authenticated) {
                Response response = request(Api.HttpMethod.GET, UserResponse.class, "user/mca/me", Map.of(
                        "token", Auth.getToken()
                ));
                if (response instanceof UserResponse userResponse) {
                    currentUser = userResponse.user();
                    likedContent.clear();
                    Collections.addAll(likedContent, currentUser.likedContent());
                    updateSearch();
                } else {
                    setError(Text.translatable("gui.skin_library.list_fetch_failed"));
                }
            }
        }).thenRunAsync(() -> {
            // fetch assets
            Response response = request(Api.HttpMethod.GET, ContentListResponse.class, "content/mca");
            if (response instanceof ContentListResponse contentListResponse) {
                content.clear();
                Collections.addAll(content, contentListResponse.contents());
                updateSearch();
            } else {
                setError(Text.translatable("gui.skin_library.list_fetch_failed"));
            }
        }).thenRunAsync(callback);
    }

    private void clearSearch() {
        filteredString = "";
        selectedTags.clear();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        switch (page) {
            case CLOTHING, HAIR -> {
                NbtCompound nbt = new NbtCompound();
                villagerVisualization.readCustomDataFromNbt(nbt);
                villagerVisualization.setBreedingAge(0);
                villagerVisualization.calculateDimensions();

                hoveredContent = null;

                int i = 0;
                for (int y = 0; y < CLOTHES_V; y++) {
                    for (int x = 0; x < CLOTHES_H + y; x++) {
                        int index = selectionPage * CLOTHES_PER_PAGE + i;
                        if (filteredContent.size() > index) {
                            LiteContent c = filteredContent.get(index);

                            Identifier identifier = SkinCache.getTextureIdentifier(c);

                            villagerVisualization.setHair(EMPTY_IDENTIFIER);
                            villagerVisualization.setClothes(identifier);

                            int cx = width / 2 + (int)((x - CLOTHES_H / 2.0 + 0.5 - 0.5 * (y % 2)) * 40);
                            int cy = height / 2 + 30 + (int)((y - CLOTHES_V / 2.0 + 0.5) * 65);

                            InventoryScreen.drawEntity(cx, cy, 26, -(mouseX - cx) / 2.0f, -(mouseY - cy) / 2.0f, villagerVisualization);

                            if (Math.abs(cx - mouseX) <= 20 && Math.abs(cy - mouseY - 30) <= 30) {
                                hoveredContent = c;
                                renderTooltip(matrices, getMetaDataText(c), mouseX, mouseY);
                            }
                            i++;
                        } else {
                            break;
                        }
                    }
                }
            }
            case EDITOR_LOCKED -> {
                drawTextBox(matrices, Text.translatable("gui.skin_library.locked"));
            }
            case EDITOR_PREPARE -> {
                drawTextBox(matrices, Text.translatable("gui.skin_library.drop"));
            }
            case EDITOR_TYPE -> {
                drawTextBox(matrices, Text.translatable("gui.skin_library.prepare"));
            }
            case EDITOR -> {
                if (shouldUpload) {
                    backendTexture.upload();
                    MinecraftClient.getInstance().getTextureManager().registerTexture(CANVAS_IDENTIFIER, backendTexture);
                    shouldUpload = false;
                }

                //painting area
                int tw = 64;
                int th = 64;
                RenderSystem.setShader(GameRenderer::getPositionShader);
                RenderSystem.setShaderTexture(0, CANVAS_IDENTIFIER);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                matrices.push();
                matrices.translate(width / 2.0f - tw * CANVAS_SCALE / 2.0f, height / 2.0f - th * CANVAS_SCALE / 2.0f, 0.0f);
                matrices.scale(CANVAS_SCALE, CANVAS_SCALE, 1.0f);

                // Calculate the clamped vertex and UV coordinates
                float vx0 = MathHelper.clamp(0, x0, x1);
                float vx1 = MathHelper.clamp(1, x0, x1);
                float vy0 = MathHelper.clamp(0, y0, y1);
                float vy1 = MathHelper.clamp(1, y0, y1);

                float uvx0 = (vx0 - x0) / (x1 - x0);
                float uvx1 = (vx1 - x0) / (x1 - x0);
                float uvy0 = (vy0 - y0) / (y1 - y0);
                float uvy1 = (vy1 - y0) / (y1 - y0);

                //draw canvas
                WidgetUtils.drawTexturedQuad(matrices.peek().getPositionMatrix(), vx0 * 64, vx1 * 64, vy0 * 64, vy1 * 64, 0, uvx0, uvx1, uvy0, uvy1);

                //border
                WidgetUtils.drawRectangle(matrices, -1, -1, tw + 1, th + 1, 0xaaffffff);

                matrices.pop();

                //hovered element
                MutableText text = Text.translatable("gui.skin_library.element.left_left");
                int textWidth = textRenderer.getWidth(text);
                renderTooltip(matrices, text, width / 2 - textWidth / 2 - 12, height / 2 - 68);

                //dummy
                villagerVisualization.limbAngle = System.currentTimeMillis() / 50.0f;
                villagerVisualization.limbDistance = 1.5f;
                villagerVisualization.setHair(EMPTY_IDENTIFIER);
                villagerVisualization.setClothes(CANVAS_IDENTIFIER);

                int cx = width / 2 + 150;
                int cy = height / 2 + 10;

                InventoryScreen.drawEntity(cx, cy, 50, -(mouseX - cx) / 2.0f, -(mouseY - cy + 32) / 2.0f, villagerVisualization);
            }
            case LOGIN -> {
                // check user authentication
                if (!awaitingAuthentication) {
                    awaitingAuthentication = true;
                    CompletableFuture.runAsync(() -> {
                        try {
                            String token = Auth.getToken();
                            Response response = token != null ? request(Api.HttpMethod.GET, IsAuthResponse.class, "auth", Map.of(
                                    "token", token
                            )) : null;
                            if (response instanceof IsAuthResponse authResponse) {
                                if (authResponse.authenticated()) {
                                    authenticated = true;
                                    clearError();
                                    reloadDatabase();

                                    //token accepted, save
                                    Auth.saveToken();

                                    setPage(Page.CLOTHING);
                                } else {
                                    //token rejected, delete file
                                    Auth.clearToken();
                                }
                            } else {
                                setError(Text.translatable("gui.skin_library.is_auth_failed"));
                            }
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        awaitingAuthentication = false;
                    });
                }

                // auth hint
                if (isBrowserOpen) {
                    drawTextBox(matrices, Text.translatable("gui.skin_library.authenticating_browser"));
                } else if (error != null) {
                    drawTextBox(matrices, Text.translatable("gui.skin_library.authenticating"));
                } else {
                    drawTextBox(matrices, Text.translatable("gui.skin_library.authenticating").append(Text.literal(" " + ".".repeat((int)(System.currentTimeMillis() / 500 % 4)))));
                }
            }
            case DETAIL -> {
                //dummy
                villagerVisualization.limbAngle = System.currentTimeMillis() / 50.0f;
                villagerVisualization.limbDistance = 1.5f;
                villagerVisualization.setHair(EMPTY_IDENTIFIER);
                villagerVisualization.setClothes(SkinCache.getTextureIdentifier(focusedContent));

                int cx = width / 2;
                int cy = height / 2 + 50;
                InventoryScreen.drawEntity(cx, cy, 60, -(mouseX - cx) / 2.0f, -(mouseY - cy) / 2.0f, villagerVisualization);

                //metadata
                renderTooltip(matrices, getMetaDataText(focusedContent), width / 2 + 200, height / 2 - 50);
            }
            case LOADING -> {
                drawTextWithShadow(matrices, textRenderer, Text.translatable("gui.loading"), width / 2, height / 2, 0xFFFFFFFF);
            }
        }

        if (error != null) {
            drawCenteredText(matrices, textRenderer, error, width / 2, height / 2, 0xFFFF0000);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private List<Text> getMetaDataText(LiteContent content) {
        SkinMeta meta = SkinCache.getMeta(content);
        if (meta == null) {
            return List.of(Text.literal(content.title()));
        } else {
            return List.of(
                    Text.literal(content.title()),
                    Text.translatable("gui.skin_library.meta.by", content.username()).formatted(Formatting.ITALIC),
                    meta.getGender() == Gender.MALE ? Text.translatable("gui.villager_editor.masculine") : (meta.getGender() == Gender.FEMALE ? Text.translatable("gui.villager_editor.feminine") : Text.translatable("gui.villager_editor.neutral")),
                    meta.getProfession() == null ? Text.translatable("entity.minecraft.villager") : Text.translatable("entity.minecraft.villager." + meta.getProfession()),
                    Text.translatable("gui.skin_library.temperature." + (meta.getTemperature() + 2)),
                    Text.translatable("gui.skin_library.meta.chance", (int)(meta.getChance() * 100))
            );
        }
    }

    private double getScreenScaleX() {
        assert client != null;
        return (double)this.client.getWindow().getScaledWidth() / (double)this.client.getWindow().getWidth();
    }

    private double getScreenScaleY() {
        assert client != null;
        return (double)this.client.getWindow().getScaledHeight() / (double)this.client.getWindow().getHeight();
    }

    private double getCanvasX() {
        assert client != null;
        double x = client.mouse.getX() * getScreenScaleX();
        return (x - width / 2.0 + 32 * CANVAS_SCALE) / CANVAS_SCALE / 64.0;
    }

    private double getCanvasY() {
        assert client != null;
        double y = client.mouse.getY() * getScreenScaleY();
        return (y - height / 2.0 + 32 * CANVAS_SCALE) / CANVAS_SCALE / 64.0;
    }

    private float getPixelX() {
        double cx = getCanvasX();
        return (int)((cx - x0) / (x1 - x0) * 64);
    }

    private float getPixelY() {
        double cy = getCanvasY();
        return (int)((cy - y0) / (y1 - y0) * 64.0);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouseMoved(mouseX, mouseY, lastMouseX - mouseX, lastMouseY - mouseY);

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        super.mouseMoved(mouseX, mouseY);
    }

    @SuppressWarnings("unused")
    protected void mouseMoved(double x, double y, double deltaX, double deltaY) {
        if (isPanning) {
            float ox = (float)(deltaX / 64 / CANVAS_SCALE);
            x0 -= ox;
            x1 -= ox;

            float oy = (float)(deltaY / 64 / CANVAS_SCALE);
            y0 -= oy;
            y1 -= oy;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            isPanning = true;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_R) {
            x0 = 0.0f;
            x1 = 1.0f;
            y0 = 0.0f;
            y1 = 1.0f;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            isPanning = false;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!isPanning && activeMouseButton >= 0 && page == Page.EDITOR) {
            int x = (int)getPixelX();
            int y = (int)getPixelY();
            ClientUtils.bethlehemLine(lastPixelMouseX, lastPixelMouseY, x, y, this::paint);
            lastPixelMouseX = x;
            lastPixelMouseY = y;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (page == Page.EDITOR) {
            float zoom = (float)(amount * 0.2f) * (x1 - x0);

            float ox = getPixelX() / 64.0f;
            x0 = x0 - zoom * ox;
            x1 = x1 + zoom * (1 - ox);

            float oy = getPixelY() / 64.0f;
            y0 = y0 - zoom * oy;
            y1 = y1 + zoom * (1 - oy);
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.EDITOR) {
            int x = (int)getPixelX();
            int y = (int)getPixelY();

            if (button == 0 || button == 1) {
                activeMouseButton = button;

                if (!isPanning) {
                    paint(x, y);
                }

                lastPixelMouseX = x;
                lastPixelMouseY = y;
            } else if (button == 2) {
                // pick color
                if (validPixel(x, y)) {
                    color.setRGB(
                            currentImage.getRed(x, y) / 255.0,
                            currentImage.getGreen(x, y) / 255.0,
                            currentImage.getBlue(x, y) / 255.0
                    );
                }
            }
        } else {
            if (hoveredContent != null) {
                if (previousScreen == null) {
                    focusedContent = hoveredContent;
                    setPage(Page.DETAIL);
                } else {
                    if (page == Page.CLOTHING) {
                        previousScreen.getVillager().setClothes("immersive_library:" + hoveredContent.contentid());
                        MinecraftClient.getInstance().setScreen(previousScreen);
                    } else if (page == Page.HAIR) {
                        previousScreen.getVillager().setHair("immersive_library:" + hoveredContent.contentid());
                        MinecraftClient.getInstance().setScreen(previousScreen);
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        activeMouseButton = -1;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean validPixel(int x, int y) {
        return x >= 0 && x < 64 && y >= 0 && y < 64;
    }

    private void paint(int x, int y) {
        if (page == Page.EDITOR && validPixel(x, y)) {
            if (activeMouseButton == 0) {
                currentImage.setColor(x, y, color.getColor());
                shouldUpload = true;
            } else if (activeMouseButton == 1) {
                currentImage.setColor(x, y, 0);
                shouldUpload = true;
            }
        }
    }

    private void drawTextBox(MatrixStack matrices, Text text) {
        List<Text> wrap = FlowingText.wrap(text, 220);
        int y = height / 2 - 20 - wrap.size() * 12;
        fill(matrices, width / 2 - 115, y - 5, width / 2 + 115, y + 12 * wrap.size(), 0x50000000);
        for (Text t : wrap) {
            drawCenteredText(matrices, textRenderer, t, width / 2, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    private void rebuild() {
        clearChildren();

        // filters
        if (page == Page.CLOTHING || page == Page.HAIR || page == Page.EDITOR_LOCKED || page == Page.EDITOR_PREPARE || page == Page.EDITOR_TYPE) {
            List<Page> b = new LinkedList<>();
            b.add(Page.CLOTHING);
            b.add(Page.HAIR);
            b.add(Page.EDITOR_PREPARE);
            b.add(Page.HELP);

            if (authenticated) {
                b.add(Page.LOGOUT);
            } else {
                b.add(Page.LOGIN);
            }

            int x = width / 2 - 20;
            int w = 220 / b.size();
            for (Page page : b) {
                addDrawableChild(new ButtonWidget(x, height / 2 - 110, w, 20, Text.translatable("gui.skin_library.page." + page.name().toLowerCase(Locale.ROOT)), sender -> setPage(page))).active = page != this.page;
                x += w;
            }
        }

        switch (page) {
            case CLOTHING, HAIR -> {
                //page
                addDrawableChild(new ButtonWidget(width / 2 - 35 - 30, height / 2 + 80, 30, 20, Text.literal("<<"), sender -> setSelectionPage(selectionPage - 1)));
                pageWidget = addDrawableChild(new ButtonWidget(width / 2 - 35, height / 2 + 80, 70, 20, Text.literal(""), sender -> {
                }));
                addDrawableChild(new ButtonWidget(width / 2 + 35, height / 2 + 80, 30, 20, Text.literal(">>"), sender -> setSelectionPage(selectionPage + 1)));
                setSelectionPage(selectionPage);

                //search
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 200 + 65, height / 2 - 110, 110, 16,
                        Text.translatable("gui.skin_library.search")));
                textFieldWidget.setMaxLength(64);
                textFieldWidget.setSuggestion("search");
                textFieldWidget.setChangedListener(s -> {
                    filteredString = s;
                    updateSearch();
                    textFieldWidget.setSuggestion(null);
                });

                //group
                addDrawableChild(CyclingButtonWidget.builder(SubscriptionFilter::getText)
                        .values(SubscriptionFilter.values())
                        .initially(subscriptionFilter)
                        .omitKeyText()
                        .build(width / 2 - 200, height / 2 - 110, 60, 20, Text.literal(""), (button, filter) -> {
                            this.subscriptionFilter = filter;
                            updateSearch();
                        }));

                //tags
                int tx = width / 2 - 200;
                for (Map.Entry<String, Long> tag : tags.entrySet()) {
                    String str = "%d x %s".formatted(tag.getValue(), tag.getKey());
                    int w = textRenderer.getWidth(str) + 6;
                    addDrawableChild(new ToggleableButtonWidget(tx, height / 2 - 110 + 22, w, 20,
                            Text.literal(str),
                            v -> {
                                if (selectedTags.contains(tag.getKey())) {
                                    selectedTags.remove(tag.getKey());
                                } else {
                                    selectedTags.add(tag.getKey());
                                }
                                ((ToggleableButtonWidget)v).toggle = !((ToggleableButtonWidget)v).toggle;
                                updateSearch();
                            })).toggle = !selectedTags.contains(tag.getKey());
                    tx += w;
                    if (tx > width / 2 + 200) {
                        break;
                    }
                }

                //controls
                int i = 0;
                for (int y = 0; y < CLOTHES_V; y++) {
                    for (int x = 0; x < CLOTHES_H + y; x++) {
                        int index = selectionPage * CLOTHES_PER_PAGE + i;
                        if (filteredContent.size() > index) {
                            LiteContent c = filteredContent.get(index);

                            int cx = width / 2 + (int)((x - CLOTHES_H / 2.0 + 0.5 - 0.5 * (y % 2)) * 40);
                            int cy = height / 2 + 25 + (int)((y - CLOTHES_V / 2.0 + 0.5) * 65);

                            drawControls(c, false, cx, cy);
                            i++;
                        } else {
                            break;
                        }
                    }
                }
            }
            case EDITOR_PREPARE -> {
                //URL
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 90, height / 2 - 18, 180, 16,
                        Text.literal("URL")));
                textFieldWidget.setMaxLength(1024);

                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 5, 100, 20, Text.translatable("gui.skin_library.load_image"), sender -> loadImage(textFieldWidget.getText())));
            }
            case EDITOR_TYPE -> {
                addDrawableChild(new ButtonWidget(width / 2 - 100, height / 2, 95, 20,
                        Text.translatable("gui.skin_library.prepare.hair"),
                        v -> {
                            settings.skinType = SkinType.HAIR;
                            setPage(Page.EDITOR);

                            // make black and white
                            if (settings.skinType == SkinType.HAIR) {
                                for (int x = 0; x < 64; x++) {
                                    for (int y = 0; y < 64; y++) {
                                        byte r = currentImage.getRed(x, y);
                                        byte g = currentImage.getGreen(x, y);
                                        byte b = currentImage.getBlue(x, y);
                                        byte l = (byte)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                                        currentImage.setColor(x, y, 255 << 24 | l << 16 | l << 18 | l);
                                    }
                                }

                                color.setHSV(0, 0, 0.5);
                            }
                        }));

                addDrawableChild(new ButtonWidget(width / 2 + 5, height / 2, 95, 20,
                        Text.translatable("gui.skin_library.prepare.clothing"),
                        v -> {
                            settings.skinType = SkinType.CLOTHING;
                            setPage(Page.EDITOR);
                        }));
            }
            case LOGIN -> {
                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 25, 100, 20,
                        Text.translatable("gui.skin_library.cancel"),
                        v -> {
                            setPage(Page.CLOTHING);
                        }));
            }
            case DETAIL -> {
                if (canModifyFocusedContent()) {
                    //tag name
                    TextFieldWidget tagNameWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 200, height / 2 - 100, 160, 16,
                            Text.literal("New Tag Name")));
                    tagNameWidget.setMaxLength(20);

                    //add tag
                    addDrawableChild(new ButtonWidget(width / 2 - 160, height / 2 - 100, 40, 20, Text.translatable("gui.skin_library.add"), sender -> {
                        String tag = tagNameWidget.getText().trim();
                        if (tag.length() > 0) {
                            setTag(focusedContent.contentid(), tag, true);
                        }
                    }));

                    //controls
                    drawControls(focusedContent, true, width / 2 + 100, height / 2 + 60);
                }

                //close
                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 60, 100, 20,
                        Text.translatable("gui.skin_library.close"),
                        v -> {
                            setPage(Page.CLOTHING);
                        }));


                //tags
                int ty = height / 2 - 70;
                for (Map.Entry<String, Long> tag : tags.entrySet()) {
                    String str = tag.getKey();
                    int w = textRenderer.getWidth(str) + 10;
                    if (canModifyFocusedContent()) {
                        addDrawableChild(new ToggleableButtonWidget(width / 2 - 200 + 20, ty, w, 20,
                                Text.literal("X"),
                                v -> {
                                    if (selectedTags.contains(tag.getKey())) {
                                        selectedTags.remove(tag.getKey());
                                    } else {
                                        selectedTags.add(tag.getKey());
                                    }
                                    v.active = !v.active;
                                    updateSearch();
                                })).active = selectedTags.contains(tag.getKey());
                    }
                    addDrawableChild(new ButtonWidget(width / 2 - 200 + 20, ty, w, 20,
                            Text.literal(str),
                            v -> {
                            }));
                    ty += 20;
                }
            }
            case EDITOR -> {
                // name
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 60, height / 2 - 105, 120, 20,
                        Text.translatable("gui.skin_library.name")));
                textFieldWidget.setMaxLength(1024);
                textFieldWidget.setText(settings.title);
                textFieldWidget.setTextFieldFocused(false);
                textFieldWidget.setChangedListener(v -> {
                    settings.title = v;
                });

                // help
                addDrawableChild(new TooltipButtonWidget(width / 2 + 65, height / 2 - 105, 20, 20,
                        Text.literal("?"),
                        Text.translatable("gui.skin_library.tool_help"),
                        v -> {
                        }));

                // chance
                addDrawableChild(new GeneSliderWidget(width / 2 - 200, height / 2 - 80, 52, 20,
                        Text.translatable("gui.skin_library.chance"),
                        1.0,
                        v -> {
                            settings.chance = v;
                        }));

                // temperature
                addDrawableChild(new IntegerSliderWidget(width / 2 - 147, height / 2 - 80, 52, 20,
                        settings.temperature,
                        -2, 2,
                        v -> {
                            settings.temperature = v;
                        },
                        v -> Text.translatable("gui.skin_library.temperature." + (v + 2))
                ));

                // gender
                addDrawableChild(new TooltipButtonWidget(width / 2 - 200, height / 2 - 60, 100 / 3, 20,
                        Text.literal("M"),
                        Text.translatable("gui.villager_editor.masculine"),
                        v -> {
                            settings.gender = Gender.MALE;
                        }));
                addDrawableChild(new TooltipButtonWidget(width / 2 - 200 + 100 / 3, height / 2 - 60, 100 / 3, 20,
                        Text.literal("N"),
                        Text.translatable("gui.villager_editor.neutral"),
                        v -> {
                            settings.gender = Gender.NEUTRAL;
                        }));
                addDrawableChild(new TooltipButtonWidget(width / 2 - 200 + 100 / 3 * 2, height / 2 - 60, 100 / 3, 20,
                        Text.literal("F"),
                        Text.translatable("gui.villager_editor.feminine"),
                        v -> {
                            settings.gender = Gender.FEMALE;
                        }));

                //profession
                int ox = 0;
                int oy = 0;
                List<ItemButtonWidget> widgets = new LinkedList<>();
                for (VillagerProfession profession : Registry.VILLAGER_PROFESSION) {
                    MutableText text = Text.translatable("entity.minecraft.villager." + profession.id());
                    ItemButtonWidget widget = addDrawableChild(new ItemButtonWidget(width / 2 - 200 + ox * 21, height / 2 - 30 + oy * 21, 20, text,
                            ProfessionIcons.ICONS.getOrDefault(profession.id(), Items.OAK_SAPLING.getDefaultStack()),
                            v -> {
                                settings.profession = profession != VillagerProfession.NONE ? null : profession.id();
                                widgets.forEach(b -> b.active = true);
                                v.active = false;
                            }));
                    widget.active = !Objects.equals(settings.profession, profession == VillagerProfession.NONE ? null : profession.id());
                    widgets.add(widget);
                    ox++;
                    if (ox >= 5) {
                        ox = 0;
                        oy++;
                    }
                }

                int y = height / 2 + 20;

                if (settings.skinType == SkinType.CLOTHING) {
                    //hue
                    hueWidget = addDrawableChild(new HorizontalColorPickerWidget(width / 2 + 100, y, 100, 15,
                            color.hue / 360.0,
                            MCA.locate("textures/colormap/hue.png"),
                            (vx, vy) -> {
                                color.setHSV(
                                        vx * 360,
                                        color.saturation,
                                        color.brightness
                                );
                            }));

                    //saturation
                    saturationWidget = addDrawableChild(new HorizontalGradientWidget(width / 2 + 100, y + 20, 100, 15,
                            color.saturation,
                            () -> {
                                double[] doubles = ClientUtils.HSV2RGB(color.hue, 0.0, 1.0);
                                return new float[] {
                                        (float)doubles[0], (float)doubles[1], (float)doubles[2], 1.0f,
                                };
                            },
                            () -> {
                                double[] doubles = ClientUtils.HSV2RGB(color.hue, 1.0, 1.0);
                                return new float[] {
                                        (float)doubles[0], (float)doubles[1], (float)doubles[2], 1.0f,
                                };
                            },
                            (vx, vy) -> {
                                color.setHSV(
                                        color.hue,
                                        vx,
                                        color.brightness
                                );
                            }));
                }

                //brightness
                brightnessWidget = addDrawableChild(new HorizontalGradientWidget(width / 2 + 100, y + 40, 100, 15,
                        color.brightness,
                        () -> {
                            double[] doubles = ClientUtils.HSV2RGB(color.hue, color.saturation, 0.0);
                            return new float[] {
                                    (float)doubles[0], (float)doubles[1], (float)doubles[2], 1.0f,
                            };
                        },
                        () -> {
                            double[] doubles = ClientUtils.HSV2RGB(color.hue, color.saturation, 1.0);
                            return new float[] {
                                    (float)doubles[0], (float)doubles[1], (float)doubles[2], 1.0f,
                            };
                        },
                        (vx, vy) -> {
                            color.setHSV(
                                    color.hue,
                                    color.saturation,
                                    vx
                            );
                        }));

                // submit
                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 80, 100, 20,
                        Text.translatable("gui.skin_library.publish"),
                        v -> {
                            submit();
                        }));
            }
        }
    }

    private void drawControls(LiteContent content, boolean advanced, int cx, int cy) {
        List<TooltipButtonWidget> widgets = new LinkedList<>();

        // subscribe
        if (isOp() || Config.getServerConfig().allowEveryoneToAddContentGlobally) {
            ToggleableTooltipButtonWidget addWidget = addDrawableChild(new ToggleableTooltipButtonWidget(0, 0, 16, 20,
                    Text.translatable("+"),
                    Text.translatable("gui.skin_library.subscribe"),
                    v -> {
                        if (((ToggleableTooltipButtonWidget)v).toggle) {
                            toListEntry(content).ifPresent(e -> {
                                NetworkHandler.sendToServer(new AddCustomClothingMessage(e));
                            });
                        } else {
                            NetworkHandler.sendToServer(new RemoveCustomClothingMessage(page == Page.CLOTHING ? RemoveCustomClothingMessage.Type.CLOTHING : RemoveCustomClothingMessage.Type.HAIR, new Identifier("immersive_library", String.valueOf(content.contentid()))));
                        }
                        ((ToggleableTooltipButtonWidget)v).toggle = !((ToggleableTooltipButtonWidget)v).toggle;
                    }));
            addWidget.toggle = getServerContentById(content.contentid()).isEmpty();
            widgets.add(addWidget);
        }

        // like
        ToggleableTooltipButtonWidget widget = addDrawableChild(new ToggleableTooltipButtonWidget(0, 0, 16, 20,
                Text.translatable("✔"),
                Text.translatable("gui.skin_library.like"),
                v -> {
                    setLike(content.contentid(), ((ToggleableTooltipButtonWidget)v).toggle);
                    ((ToggleableTooltipButtonWidget)v).toggle = !((ToggleableTooltipButtonWidget)v).toggle;
                }));
        widget.toggle = !isLiked(content);
        widgets.add(widget);

        // delete
        if (advanced && currentUser != null && currentUser.moderator()) {
            TooltipButtonWidget deleteWidget = addDrawableChild(new TooltipButtonWidget(cx - 12 + 25, cy, 16, 20,
                    Text.translatable("\uD83D\uDD28"),
                    Text.translatable("gui.skin_library.delete"),
                    v -> {
                        removeContent(content.userid());
                    }));
            widgets.add(deleteWidget);
        }

        // add the widgets
        int wx = cx - widgets.size() * 8;
        for (TooltipButtonWidget buttonWidget : widgets) {
            addDrawableChild(buttonWidget);
            buttonWidget.x = wx;
            buttonWidget.y = cy;
            wx += 16;
        }
    }

    private Optional<SkinListEntry> toListEntry(LiteContent content) {
        SkinMeta meta = SkinCache.getMeta(content);
        if (meta != null) {
            if (content.hasTag("clothing")) {
                return Optional.of(new Clothing("immersive_library:" + content.contentid(), meta.getProfession(), meta.getTemperature(), false));
            } else {
                return Optional.of(new Hair("immersive_library:" + content.contentid()));
            }
        }
        return Optional.empty();
    }

    private boolean canModifyFocusedContent() {
        return currentUser != null && (currentUser.moderator() || currentUser.userid() == focusedContent.userid());
    }

    private boolean isLiked(LiteContent content) {
        return likedContent.stream().anyMatch(c -> c.contentid() == content.contentid());
    }

    public void setPage(Page page) {
        if (Thread.currentThread() != thread) {
            assert client != null;
            client.executeSync(() -> setPage(page));
            return;
        }

        clearError();

        if ((page == Page.EDITOR_TYPE || page == Page.EDITOR_PREPARE) && !authenticated) {
            setPage(Page.EDITOR_LOCKED);
            return;
        }

        if (page == Page.HELP) {
            try {
                Util.getOperatingSystem().open(URI.create("https://github.com/Luke100000/minecraft-comes-alive/wiki/Skin-Editor"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (page == Page.LOGIN) {
            if (Auth.loadToken() == null) {
                isBrowserOpen = true;
                Auth.authenticate(getPlayerName());
            } else {
                isBrowserOpen = false;
            }
        }

        if (page == Page.LOGOUT) {
            authenticated = false;
            Auth.clearToken();
            refreshPage();
            return;
        }

        if (page != this.page) {
            clearSearch();
        }

        this.page = page;

        if (page == Page.EDITOR) {
            x0 = 0.0f;
            x1 = 1.0f;
            y0 = 0.0f;
            y1 = 1.0f;
        }

        if (page == Page.CLOTHING || page == Page.HAIR) {
            updateSearch();
        } else {
            rebuild();
        }
    }

    private void updateSearch() {
        if (Thread.currentThread() != thread) {
            assert client != null;
            client.executeSync(this::updateSearch);
            return;
        }

        // fetch the list matching the current subscription filter
        List<LiteContent> contents = subscriptionFilter == SubscriptionFilter.LIBRARY ? content : (subscriptionFilter == SubscriptionFilter.LIKED ? likedContent : serverContent);

        // pre-filter the assets by string search and asset group
        List<LiteContent> untaggedFilteredContent = contents.stream()
                .filter(v -> filteredString.isEmpty() || v.title().contains(filteredString))
                .filter(v -> v.hasTag(page == Page.CLOTHING ? "clothing" : "hair"))
                .toList();

        // extract all tags, count them and sort them
        tags = untaggedFilteredContent.stream()
                .flatMap(v -> Arrays.stream(v.tags()))
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        // now filter by selected tags
        filteredContent = untaggedFilteredContent.stream()
                .filter(v -> selectedTags.size() == 0 || v.hasTag(selectedTags))
                .toList();

        // load meta in the background
        filteredContent.forEach(SkinCache::getMeta);

        rebuild();

        setSelectionPage(selectionPage);
    }

    private String getPlayerName() {
        return MinecraftClient.getInstance().player == null ? "Unknown" : MinecraftClient.getInstance().player.getGameProfile().getName();
    }

    private boolean isOp() {
        return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.hasPermissionLevel(4);
    }

    private void setSelectionPage(int p) {
        if (pageWidget != null) {
            selectionPage = Math.max(0, Math.min(getMaxPages() - 1, p));
            pageWidget.setMessage(Text.literal((selectionPage + 1) + " / " + getMaxPages()));
        }
    }

    private int getMaxPages() {
        return (int)Math.ceil(filteredContent.size() / 24.0);
    }

    @Override
    public void filesDragged(List<Path> paths) {
        Path path = paths.get(0);
        loadImage(path.toString());
    }

    private void loadImage(String path) {
        InputStream stream = null;
        try {
            stream = new URL(path).openStream();
        } catch (Exception exception) {
            try {
                stream = new FileInputStream(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (stream != null) {
            try {
                currentImage = NativeImage.read(stream);

                if (currentImage.getWidth() != 64 || currentImage.getHeight() != 64) {
                    setError(Text.translatable("gui.skin_library.not_64"));
                    currentImage = null;
                }

                backendTexture = new NativeImageBackedTexture(currentImage);
                stream.close();

                shouldUpload = true;
                settings = new WorkspaceSettings();
                setPage(Page.EDITOR_TYPE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void submit() {
        if (!uploading) {
            uploading = true;
            CompletableFuture.runAsync(() -> {
                if (Auth.getToken() != null) {
                    Response request = null;
                    try {
                        request = request(settings.contentid == -1 ? Api.HttpMethod.POST : Api.HttpMethod.PUT, ContentIdResponse.class, settings.contentid == -1 ? "content/mca" : "content/mca/%s".formatted(settings.contentid), Map.of(
                                "token", Auth.getToken()
                        ), Map.of(
                                "title", settings.title,
                                "meta", "{}",
                                "data", new String(Base64.getEncoder().encode(currentImage.getBytes()))
                        ));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (request instanceof ContentIdResponse response) {
                        reloadDatabase(() -> {
                            int contentid = response.contentid();

                            // default tags
                            setTag(contentid, settings.skinType.name().toLowerCase(Locale.ROOT), true);
                            setTag(contentid, settings.profession == null ? "generic" : settings.profession, true);

                            // open detail page
                            getContentById(contentid).ifPresent(content -> {
                                focusedContent = content;
                                setPage(Page.DETAIL);
                            });
                        });
                    } else if (request instanceof ErrorResponse response) {
                        setError(Text.of(response.message()));
                    }
                }
                uploading = false;
            });
        } else {
            setError(Text.translatable("gui.skin_library.already_uploading"));
        }
    }

    private Optional<LiteContent> getContentById(int contentid) {
        return content.stream().filter(v -> v.contentid() == contentid).findAny();
    }

    private Optional<LiteContent> getServerContentById(int contentid) {
        return serverContent.stream().filter(v -> v.contentid() == contentid).findAny();
    }

    private void setTag(int contentid, String tag, boolean add) {
        if (Auth.getToken() != null) {
            request(add ? Api.HttpMethod.POST : Api.HttpMethod.DELETE, SuccessResponse.class, "tag/mca/%s/%s".formatted(contentid, tag), Map.of(
                    "token", Auth.getToken()
            ));
        }
    }

    private void removeContent(int contentId) {
        if (Auth.getToken() != null) {
            request(Api.HttpMethod.DELETE, SuccessResponse.class, "content/mca/%s".formatted(contentId), Map.of(
                    "token", Auth.getToken()
            ));
        }
    }

    private void setLike(int contentid, boolean add) {
        if (Auth.getToken() != null) {
            request(add ? Api.HttpMethod.POST : Api.HttpMethod.DELETE, SuccessResponse.class, "like/mca/%s".formatted(contentid), Map.of(
                    "token", Auth.getToken()
            ));
        }
    }

    public void refreshPage() {
        setPage(page);
    }

    public void clearError() {
        this.error = null;
    }

    public void setError(Text text) {
        this.error = text;
    }

    private <T> void addServerContent(Map<String, T> map, String type) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getKey().startsWith("immersive_library:")) {
                try {
                    int contentid = Integer.parseInt(entry.getKey().substring(18));
                    serverContent.add(getContentById(contentid).orElse(new LiteContent(
                            contentid, -1, "unknown", -1, new String[] {type}, "unknown", -1
                    )));
                } catch (NumberFormatException ignored) {

                }
            }
        }
    }

    @Override
    public void skinListUpdatedCallback() {
        refreshServerContent();

        if (page == Page.CLOTHING || page == Page.HAIR) {
            updateSearch();
        }
    }

    private void refreshServerContent() {
        serverContent.clear();
        addServerContent(VillagerEditorScreen.getClothing(), "clothing");
        addServerContent(VillagerEditorScreen.getHair(), "hair");
    }

    public enum Page {
        CLOTHING,
        HAIR,
        EDITOR_LOCKED,
        EDITOR_PREPARE,
        EDITOR_TYPE,
        EDITOR,
        HELP,
        LOGIN,
        LOGOUT,
        DETAIL,
        LOADING
    }

    public enum SkinType {
        CLOTHING,
        HAIR
    }

    public enum SubscriptionFilter {
        LIBRARY,
        LIKED,
        GLOBAL;

        public static Text getText(SubscriptionFilter t) {
            return Text.translatable("gui.skin_library.subscription_filter." + t.name().toLowerCase(Locale.ROOT));
        }
    }

    public static final class WorkspaceSettings {
        public int contentid = -1;

        public int temperature;
        public double chance = 1.0;
        public String title = "Unnamed Asset";
        public String profession;
        public SkinType skinType;
        public Gender gender = Gender.NEUTRAL;

        public WorkspaceSettings() {

        }
    }

    static class ColorSelector {
        double red, green, blue;
        double hue, saturation, brightness;

        public ColorSelector() {
            setHSV(0.5, 0.5, 0.5);
        }

        public void setRGB(double red, double green, double blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            updateHSV();
        }

        public void setHSV(double hue, double saturation, double brightness) {
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
            updateRGB();

            if (hueWidget != null) {
                hueWidget.setValueX(hue / 360.0);
                saturationWidget.setValueX(saturation);
                brightnessWidget.setValueX(brightness);
            }
        }

        private void updateRGB() {
            double[] doubles = ClientUtils.HSV2RGB(hue, saturation, brightness);
            this.red = doubles[0];
            this.green = doubles[1];
            this.blue = doubles[2];
        }

        private void updateHSV() {
            double[] doubles = ClientUtils.RGB2HSV(red, green, blue);
            this.hue = doubles[0];
            this.saturation = doubles[1];
            this.brightness = doubles[2];

            if (hueWidget != null) {
                hueWidget.setValueX(hue / 360.0);
                saturationWidget.setValueX(saturation);
                brightnessWidget.setValueX(brightness);
            }
        }

        public int getRed() {
            return (int)(red * 255);
        }

        public int getGreen() {
            return (int)(green * 255);
        }

        public int getBlue() {
            return (int)(blue * 255);
        }

        public int getColor() {
            return 0xFF000000 | getBlue() << 16 | getGreen() << 8 | getRed();
        }
    }
}
