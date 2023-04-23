package net.mca.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mca.Config;
import net.mca.MCA;
import net.mca.client.gui.immersiveLibrary.Api;
import net.mca.client.gui.immersiveLibrary.Auth;
import net.mca.client.gui.immersiveLibrary.SkinCache;
import net.mca.client.gui.immersiveLibrary.Workspace;
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

import javax.annotation.Nullable;
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
    private final List<LiteContent> serverContent = new ArrayList<>();
    private List<LiteContent> filteredContent = new ArrayList<>();
    private SubscriptionFilter subscriptionFilter = SubscriptionFilter.LIBRARY;

    @Nullable
    private User currentUser;

    private int selectionPage;
    private LiteContent focusedContent;
    private LiteContent hoveredContent;
    private Page page;

    private ButtonWidget pageWidget;

    private Workspace workspace;

    private final ColorSelector color = new ColorSelector();
    private int activeMouseButton;
    private int lastPixelMouseX;
    private int lastPixelMouseY;

    private float x0, x1, y0, y1;
    private boolean isPanning;
    private boolean hasPanned;
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
                setPage(Page.LIBRARY);
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
        hoveredContent = null;

        switch (page) {
            case LIBRARY -> {
                NbtCompound nbt = new NbtCompound();
                villagerVisualization.readCustomDataFromNbt(nbt);
                villagerVisualization.setBreedingAge(0);
                villagerVisualization.calculateDimensions();

                int i = 0;
                for (int y = 0; y < CLOTHES_V; y++) {
                    for (int x = 0; x < CLOTHES_H + y; x++) {
                        int index = selectionPage * CLOTHES_PER_PAGE + i;
                        if (filteredContent.size() > index) {
                            LiteContent c = filteredContent.get(index);

                            setDummyTexture(c);

                            int cx = width / 2 + (int)((x - CLOTHES_H / 2.0 + 0.5 - 0.5 * (y % 2)) * 40);
                            int cy = height / 2 + 22 + (int)((y - CLOTHES_V / 2.0 + 0.5) * 65);

                            if (Math.abs(cx - mouseX) <= 20 && Math.abs(cy - mouseY - 25) <= 24) {
                                hoveredContent = c;
                                renderTooltip(matrices, getMetaDataText(c), mouseX, mouseY);
                            }

                            InventoryScreen.drawEntity(cx, cy, hoveredContent == c ? 30 : 26, -(mouseX - cx) / 2.0f, -(mouseY - cy) / 2.0f, villagerVisualization);
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
                if (workspace.dirty) {
                    workspace.backendTexture.upload();
                    MinecraftClient.getInstance().getTextureManager().registerTexture(CANVAS_IDENTIFIER, workspace.backendTexture);
                    workspace.dirty = false;
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

                if (workspace.skinType == SkinType.CLOTHING) {
                    villagerVisualization.setHair(EMPTY_IDENTIFIER);
                    villagerVisualization.setClothes(CANVAS_IDENTIFIER);
                } else {
                    villagerVisualization.setHair(CANVAS_IDENTIFIER);
                    villagerVisualization.setClothes(EMPTY_IDENTIFIER);
                }

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

                                    setPage(Page.LIBRARY);
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

                setDummyTexture(focusedContent);

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

    private void setDummyTexture(LiteContent content) {
        if (content.hasTag("clothing")) {
            villagerVisualization.setHair(EMPTY_IDENTIFIER);
            villagerVisualization.setClothes(SkinCache.getTextureIdentifier(content));
        } else {
            villagerVisualization.setHair(SkinCache.getTextureIdentifier(content));
            villagerVisualization.setClothes(EMPTY_IDENTIFIER);
        }
    }

    private List<Text> getMetaDataText(LiteContent content) {
        SkinMeta meta = SkinCache.getMeta(content);
        if (meta == null) {
            return List.of(Text.literal(content.title()));
        } else {
            return List.of(
                    Text.literal(content.title()),
                    Text.translatable("gui.skin_library.meta.by", content.username()).formatted(Formatting.ITALIC),
                    Text.translatable("gui.skin_library.gender", meta.getGender() == Gender.MALE ? Text.translatable("gui.villager_editor.masculine") : (meta.getGender() == Gender.FEMALE ? Text.translatable("gui.villager_editor.feminine") : Text.translatable("gui.villager_editor.neutral"))),
                    Text.translatable("gui.skin_library.profession", meta.getProfession() == null ? Text.translatable("entity.minecraft.villager") : Text.translatable("entity.minecraft.villager." + meta.getProfession())),
                    Text.translatable("gui.skin_library.temperature", Text.translatable("gui.skin_library.temperature." + (meta.getTemperature() + 2))),
                    Text.translatable("gui.skin_library.chance_val", (int)(meta.getChance() * 100)).formatted(Formatting.GRAY),
                    Text.literal(String.join(", ", content.tags())).formatted(Formatting.YELLOW)
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

            hasPanned = true;
        } else {
            hasPanned = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Pan
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            isPanning = true;
            return true;
        }

        // Reset viewport
        if (keyCode == GLFW.GLFW_KEY_R) {
            x0 = 0.0f;
            x1 = 1.0f;
            y0 = 0.0f;
            y1 = 1.0f;
            return true;
        }

        // Fill-delete
        if (keyCode == GLFW.GLFW_KEY_F) {
            workspace.fillDelete((int)getPixelX(), (int)getPixelY());
            return true;
        }

        // Color pick
        if (keyCode == GLFW.GLFW_KEY_P) {
            pickColor();
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
            if (button == 0 || button == 1) {
                activeMouseButton = button;

                int x = (int)getPixelX();
                int y = (int)getPixelY();

                if (!isPanning) {
                    paint(x, y);
                }

                lastPixelMouseX = x;
                lastPixelMouseY = y;
            } else if (button == 2) {
                isPanning = true;
            }
        } else {
            if (hoveredContent != null) {
                if (previousScreen == null) {
                    focusedContent = hoveredContent;
                    setPage(Page.DETAIL);
                } else {
                    if (hoveredContent.hasTag("clothing")) {
                        previousScreen.getVillager().setClothes("immersive_library:" + hoveredContent.contentid());
                        MinecraftClient.getInstance().setScreen(previousScreen);
                    } else if (hoveredContent.hasTag("hair")) {
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

        if (button == 2) {
            isPanning = false;
            if (!hasPanned && page == Page.EDITOR) {
                pickColor();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
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

    private void paint(int x, int y) {
        if (page == SkinLibraryScreen.Page.EDITOR && workspace.validPixel(x, y)) {
            if (activeMouseButton == 0) {
                workspace.currentImage.setColor(x, y, color.getColor());
                workspace.dirty = true;
            } else if (activeMouseButton == 1) {
                workspace.currentImage.setColor(x, y, 0);
                workspace.dirty = true;
            }
        }
    }

    private void pickColor() {
        int x = (int)getPixelX();
        int y = (int)getPixelY();
        if (workspace.validPixel(x, y)) {
            color.setRGB(
                    workspace.currentImage.getRed(x, y) / 255.0,
                    workspace.currentImage.getGreen(x, y) / 255.0,
                    workspace.currentImage.getBlue(x, y) / 255.0
            );
        }
    }

    private void rebuild() {
        clearChildren();

        // filters
        if (page == Page.LIBRARY || page == Page.EDITOR_LOCKED || page == Page.EDITOR_PREPARE || page == Page.EDITOR_TYPE) {
            List<Page> b = new LinkedList<>();
            b.add(Page.LIBRARY);
            b.add(Page.EDITOR_PREPARE);
            b.add(Page.HELP);

            if (authenticated) {
                b.add(Page.LOGOUT);
            } else {
                b.add(Page.LOGIN);
            }

            int x = page == Page.LIBRARY ? width / 2 - 20 : width / 2 - 110;
            int w = 220 / b.size();
            for (Page page : b) {
                addDrawableChild(new ButtonWidget(x, height / 2 - 110, w, 20, Text.translatable("gui.skin_library.page." + page.name().toLowerCase(Locale.ROOT)), sender -> setPage(page))).active = page != this.page;
                x += w;
            }
        }

        switch (page) {
            case LIBRARY -> {
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
                textFieldWidget.setText(filteredString);
                textFieldWidget.setChangedListener(s -> {
                    filteredString = s;
                    updateSearch();
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
                    int w = textRenderer.getWidth(str) + 8;
                    addDrawableChild(new ToggleableButtonWidget(tx, height / 2 - 110 + 22, w, 20,
                            selectedTags.contains(tag.getKey()),
                            Text.literal(str),
                            v -> {
                                if (selectedTags.contains(tag.getKey())) {
                                    selectedTags.remove(tag.getKey());
                                } else {
                                    selectedTags.add(tag.getKey());
                                }
                                ((ToggleableButtonWidget)v).toggle = !((ToggleableButtonWidget)v).toggle;
                                updateSearch();
                            }));
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
                            workspace.skinType = SkinType.HAIR;
                            setPage(Page.EDITOR);

                            // make black and white
                            if (workspace.skinType == SkinType.HAIR) {
                                double maxLuminance = 0.0;
                                for (int x = 0; x < 64; x++) {
                                    for (int y = 0; y < 64; y++) {
                                        int r = workspace.currentImage.getRed(x, y);
                                        int g = workspace.currentImage.getGreen(x, y);
                                        int b = workspace.currentImage.getBlue(x, y);
                                        double l = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                                        maxLuminance = Math.max(maxLuminance, l);
                                    }
                                }
                                for (int x = 0; x < 64; x++) {
                                    for (int y = 0; y < 64; y++) {
                                        int r = workspace.currentImage.getRed(x, y);
                                        int g = workspace.currentImage.getGreen(x, y);
                                        int b = workspace.currentImage.getBlue(x, y);
                                        int a = workspace.currentImage.getOpacity(x, y);
                                        int l = (int)((0.2126 * r + 0.7152 * g + 0.0722 * b + (255.0 - maxLuminance)) * 0.5 + 128);
                                        workspace.currentImage.setColor(x, y, a << 24 | l << 16 | l << 8 | l);
                                    }
                                }

                                color.setHSV(0, 0, 0.5);
                            }
                        }));

                addDrawableChild(new ButtonWidget(width / 2 + 5, height / 2, 95, 20,
                        Text.translatable("gui.skin_library.prepare.clothing"),
                        v -> {
                            workspace.skinType = SkinType.CLOTHING;
                            setPage(Page.EDITOR);
                        }));
            }
            case LOGIN -> {
                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 25, 100, 20,
                        Text.translatable("gui.skin_library.cancel"),
                        v -> {
                            setPage(Page.LIBRARY);
                        }));
            }
            case DETAIL -> {
                if (canModifyFocusedContent()) {
                    //tag name
                    TextFieldWidget tagNameWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 200, height / 2 - 100 + 2, 95, 16,
                            Text.literal("New Tag Name")));
                    tagNameWidget.setMaxLength(20);

                    //add tag
                    addDrawableChild(new ButtonWidget(width / 2 - 100, height / 2 - 100, 40, 20, Text.translatable("gui.skin_library.add"), sender -> {
                        String tag = tagNameWidget.getText().trim();
                        if (tag.length() > 0) {
                            setTag(focusedContent.contentid(), tag, true);
                            focusedContent.tags().add(tag);
                            tagNameWidget.setText("");
                            rebuild();
                        }
                    }));

                    //controls
                    drawControls(focusedContent, true, width / 2 + 100, height / 2 + 60);
                }

                //close
                addDrawableChild(new ButtonWidget(width / 2 - 50, height / 2 + 60, 100, 20,
                        Text.translatable("gui.skin_library.close"),
                        v -> {
                            setPage(Page.LIBRARY);
                        }));


                //tags
                int ty = height / 2 - 70;
                for (String tag : focusedContent.tags()) {
                    // todo blacklisted tags could be a bit more smooth, maybe include professions
                    if (!tag.equals("clothing") && !tag.equals("hair")) {
                        int w = textRenderer.getWidth(tag) + 10;
                        if (canModifyFocusedContent()) {
                            addDrawableChild(new ButtonWidget(width / 2 - 200, ty, 20, 20,
                                    Text.literal("X"),
                                    v -> {
                                        setTag(focusedContent.contentid(), tag, false);
                                        focusedContent.tags().remove(tag);
                                    }));
                        }
                        addDrawableChild(new ButtonWidget(width / 2 - 200 + 20, ty, w, 20,
                                Text.literal(tag),
                                v -> {
                                }));
                        ty += 20;
                    }
                }
            }
            case EDITOR -> {
                // name
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 60, height / 2 - 105, 120, 20,
                        Text.translatable("gui.skin_library.name")));
                textFieldWidget.setMaxLength(1024);
                textFieldWidget.setText(workspace.title);
                textFieldWidget.setTextFieldFocused(false);
                textFieldWidget.setChangedListener(v -> {
                    workspace.title = v;
                });

                // help
                addDrawableChild(new TooltipButtonWidget(width / 2 + 65, height / 2 - 105, 20, 20,
                        Text.literal("?"),
                        Text.translatable("gui.skin_library.tool_help"),
                        v -> {
                            openHelp();
                        }));

                // chance
                addDrawableChild(new DoubleSliderWidget(width / 2 - 200, height / 2 - 80, 52, 20,
                        workspace.chance,
                        0.0, 2.0,
                        v -> {
                            workspace.chance = v;
                        },
                        v -> Text.translatable("gui.skin_library.meta.chance"),
                        () -> Text.translatable("gui.skin_library.meta.chance.tooltip")));

                //gender
                addDrawableChild(CyclingButtonWidget.builder(Gender::getText)
                        .values(Gender.MALE, Gender.NEUTRAL, Gender.FEMALE)
                        .initially(workspace.gender)
                        .omitKeyText()
                        .build(width / 2 - 147, height / 2 - 80, 52, 20, Text.literal(""), (button, gender) -> {
                            this.workspace.gender = gender;
                            updateSearch();
                        }));

                // temperature
                if (workspace.skinType == SkinType.CLOTHING) {
                    addDrawableChild(new IntegerSliderWidget(width / 2 - 200, height / 2 - 60, 105, 20,
                            workspace.temperature,
                            -2, 2,
                            v -> {
                                workspace.temperature = v;
                            },
                            v -> Text.translatable("gui.skin_library.temperature." + (v + 2)),
                            () -> Text.translatable("gui.skin_library.temperature.tooltip")
                    ));
                }

                //profession
                if (workspace.skinType == SkinType.CLOTHING) {
                    int ox = 0;
                    int oy = 0;
                    List<ItemButtonWidget> widgets = new LinkedList<>();
                    for (VillagerProfession profession : Registry.VILLAGER_PROFESSION) {
                        MutableText text = Text.translatable("entity.minecraft.villager." + profession.id());
                        ItemButtonWidget widget = addDrawableChild(new ItemButtonWidget(width / 2 - 200 + ox * 21, height / 2 - 30 + oy * 21, 20, text,
                                ProfessionIcons.ICONS.getOrDefault(profession.id(), Items.OAK_SAPLING.getDefaultStack()),
                                v -> {
                                    workspace.profession = profession != VillagerProfession.NONE ? null : profession.id();
                                    widgets.forEach(b -> b.active = true);
                                    v.active = false;
                                }));
                        widget.active = !Objects.equals(workspace.profession, profession == VillagerProfession.NONE ? null : profession.id());
                        widgets.add(widget);
                        ox++;
                        if (ox >= 5) {
                            ox = 0;
                            oy++;
                        }
                    }
                }

                int y = height / 2 + 20;

                if (workspace.skinType == SkinType.CLOTHING) {
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

                //fill tool strength
                addDrawableChild(new IntegerSliderWidget(width / 2 + 100, y + 60, 100, 20,
                        workspace.fillToolThreshold,
                        0, 32,
                        v -> {
                            workspace.fillToolThreshold = v;
                        },
                        v -> Text.translatable("gui.skin_library.fillToolThreshold", +v),
                        () -> Text.translatable("gui.skin_library.fillToolThreshold.tooltip")
                ));

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
        int w = advanced ? 25 : 18;
        List<TooltipButtonWidget> widgets = new LinkedList<>();

        // subscribe
        if (isOp() || Config.getServerConfig().allowEveryoneToAddContentGlobally) {
            widgets.add(addDrawableChild(new ToggleableTooltipButtonWidget(0, 0, w, 20,
                    getServerContentById(content.contentid()).isPresent(),
                    Text.translatable("+"),
                    Text.translatable("gui.skin_library.subscribe"),
                    v -> {
                        if (((ToggleableTooltipButtonWidget)v).toggle) {
                            NetworkHandler.sendToServer(new RemoveCustomClothingMessage(content.hasTag("clothing") ? RemoveCustomClothingMessage.Type.CLOTHING : RemoveCustomClothingMessage.Type.HAIR, new Identifier("immersive_library", String.valueOf(content.contentid()))));
                        } else {
                            toListEntry(content).ifPresent(e -> {
                                NetworkHandler.sendToServer(new AddCustomClothingMessage(e));
                            });
                        }
                        ((ToggleableTooltipButtonWidget)v).toggle = !((ToggleableTooltipButtonWidget)v).toggle;
                    })));
        }

        // like
        widgets.add(addDrawableChild(new ToggleableTooltipButtonWidget(0, 0, w, 20,
                isLiked(content),
                Text.translatable("✔"),
                Text.translatable("gui.skin_library.like"),
                v -> {
                    ((ToggleableTooltipButtonWidget)v).toggle = !((ToggleableTooltipButtonWidget)v).toggle;
                    setLike(content.contentid(), ((ToggleableTooltipButtonWidget)v).toggle);
                })));

        // delete
        if (advanced && canModifyContent(content)) {
            widgets.add(addDrawableChild(new TooltipButtonWidget(cx - 12 + 25, cy, w, 20,
                    Text.translatable("X"),
                    Text.translatable("gui.skin_library.delete"),
                    v -> {
                        removeContent(content.contentid());
                        setPage(Page.LIBRARY);
                    })));
        }

        // add the widgets
        int wx = cx - widgets.size() * w / 2;
        for (TooltipButtonWidget buttonWidget : widgets) {
            addDrawableChild(buttonWidget);
            buttonWidget.x = wx;
            buttonWidget.y = cy;
            wx += w;
        }
    }

    private Optional<SkinListEntry> toListEntry(LiteContent content) {
        SkinMeta meta = SkinCache.getMeta(content);
        if (meta != null) {
            if (content.hasTag("clothing")) {
                return Optional.of(new Clothing("immersive_library:" + content.contentid(), meta.getProfession(), meta.getTemperature(), false, meta.getGender()));
            } else {
                return Optional.of(new Hair("immersive_library:" + content.contentid()));
            }
        }
        return Optional.empty();
    }

    private boolean canModifyFocusedContent() {
        return canModifyContent(focusedContent);
    }

    private boolean canModifyContent(LiteContent content) {
        return currentUser != null && (currentUser.moderator() || currentUser.userid() == content.userid());
    }

    private boolean isLiked(LiteContent content) {
        return currentUser != null && (currentUser.likes().stream().anyMatch(c -> c.contentid() == content.contentid()));
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
            openHelp();
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

        if (page == Page.LIBRARY) {
            updateSearch();
        } else {
            rebuild();
        }
    }

    private void openHelp() {
        try {
            Util.getOperatingSystem().open(URI.create("https://github.com/Luke100000/minecraft-comes-alive/wiki/Skin-Editor"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSearch() {
        if (Thread.currentThread() != thread) {
            assert client != null;
            client.executeSync(this::updateSearch);
            return;
        }

        refreshServerContent();

        // fetch the list matching the current subscription filter
        List<LiteContent> contents = Collections.emptyList();
        switch (subscriptionFilter) {
            case LIBRARY -> {contents = content;}
            case GLOBAL -> {contents = serverContent;}
            case LIKED -> {contents = currentUser != null ? currentUser.likes() : Collections.emptyList();}
            case SUBMISSIONS -> {contents = currentUser != null ? currentUser.submissions() : Collections.emptyList();}
        }

        // pre-filter the assets by string search and asset group
        List<LiteContent> untaggedFilteredContent = contents.stream()
                .filter(v -> (filteredString.isEmpty() || v.title().contains(filteredString)) || v.tags().stream().anyMatch(t -> t.contains(filteredString)))
                .toList();

        // extract all tags, count them and sort them
        tags = untaggedFilteredContent.stream()
                .flatMap(v -> v.tags().stream())
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
                NativeImage image = NativeImage.read(stream);
                stream.close();

                if (image.getWidth() != 64 || image.getHeight() != 64) {
                    setError(Text.translatable("gui.skin_library.not_64"));
                } else {
                    workspace = new Workspace(image);

                    setPage(Page.EDITOR_TYPE);
                }
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
                        request = request(workspace.contentid == -1 ? Api.HttpMethod.POST : Api.HttpMethod.PUT, ContentIdResponse.class, workspace.contentid == -1 ? "content/mca" : "content/mca/%s".formatted(workspace.contentid), Map.of(
                                "token", Auth.getToken()
                        ), Map.of(
                                "title", workspace.title,
                                "meta", workspace.toListEntry().toJson().toString(),
                                "data", new String(Base64.getEncoder().encode(workspace.currentImage.getBytes()))
                        ));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (request instanceof ContentIdResponse response) {
                        reloadDatabase(() -> {
                            int contentid = response.contentid();

                            // default tags
                            setTag(contentid, workspace.skinType.name().toLowerCase(Locale.ROOT), true);
                            setTag(contentid, workspace.profession == null ? "generic" : workspace.profession, true);

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
            content.removeIf(v -> v.contentid() == contentId);

            if (currentUser != null) {
                currentUser.likes().removeIf(v -> v.contentid() == contentId);
                currentUser.submissions().removeIf(v -> v.contentid() == contentId);
            }
        }
    }

    private void setLike(int contentid, boolean add) {
        if (Auth.getToken() != null && currentUser != null) {
            request(add ? Api.HttpMethod.POST : Api.HttpMethod.DELETE, SuccessResponse.class, "like/mca/%s".formatted(contentid), Map.of(
                    "token", Auth.getToken()
            ));

            if (add) {
                getContentById(contentid).ifPresent(currentUser.likes()::add);
            } else {
                currentUser.likes().removeIf(v -> v.contentid() == contentid);
            }
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
                            contentid, -1, "unknown", -1, Set.of(type), "unknown", -1
                    )));
                } catch (NumberFormatException ignored) {

                }
            }
        }
    }

    @Override
    public void skinListUpdatedCallback() {
        refreshServerContent();

        if (page == Page.LIBRARY) {
            updateSearch();
        }
    }

    private void refreshServerContent() {
        serverContent.clear();
        addServerContent(VillagerEditorScreen.getClothing(), "clothing");
        addServerContent(VillagerEditorScreen.getHair(), "hair");
    }

    public enum Page {
        LIBRARY,
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
        GLOBAL,
        LIKED,
        SUBMISSIONS;

        public static Text getText(SubscriptionFilter t) {
            return Text.translatable("gui.skin_library.subscription_filter." + t.name().toLowerCase(Locale.ROOT));
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
