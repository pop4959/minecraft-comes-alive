package net.mca.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.mca.MCA;
import net.mca.client.gui.immersiveLibrary.Api;
import net.mca.client.gui.immersiveLibrary.Auth;
import net.mca.client.gui.immersiveLibrary.SkinCache;
import net.mca.client.gui.immersiveLibrary.responses.*;
import net.mca.client.gui.immersiveLibrary.types.Content;
import net.mca.client.gui.widget.*;
import net.mca.client.resources.ByteImage;
import net.mca.client.resources.ClientUtils;
import net.mca.entity.EntitiesMCA;
import net.mca.entity.VillagerEntityMCA;
import net.mca.util.localization.FlowingText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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

import static net.mca.client.gui.immersiveLibrary.Api.request;

public class SkinLibraryScreen extends Screen {
    private static final Identifier EMPTY_IDENTIFIER = MCA.locate("skins/empty.png");
    private static final Identifier CANVAS_IDENTIFIER = MCA.locate("temp");
    private static final float CANVAS_SCALE = 2.35f;

    private String filteredString = "";
    private final List<String> tags = new LinkedList<>();
    private final Set<String> filteredTags = new HashSet<>();
    private final List<Content> content = new ArrayList<>();
    private final List<Content> serverContent = new ArrayList<>();
    private final List<Content> filteredContent = new ArrayList<>();
    private SubscriptionFilter subscriptionFilter = SubscriptionFilter.LIBRARY;

    private int selectionPage;
    private int focusedSkin;
    private Page page;

    private ButtonWidget pageWidget;

    private ByteImage currentImage;
    private WorkspaceSettings settings;
    private boolean shouldUpload = true;

    private final colorSelector color = new colorSelector();
    private int activeMouseButton;
    private int lastPixelMouseX;
    private int lastPixelMouseY;

    private float x0, x1, y0, y1;
    private boolean isPanning;
    private double lastMouseX;
    private double lastMouseY;

    private Text error;

    protected final VillagerEntityMCA villagerVisualization = Objects.requireNonNull(EntitiesMCA.MALE_VILLAGER.get().create(MinecraftClient.getInstance().world));

    int CLOTHES_H = 10;
    int CLOTHES_V = 2;
    int CLOTHES_PER_PAGE = CLOTHES_H * CLOTHES_V + 1; // todo what

    private boolean authenticated = false;
    private boolean awaitingAuthentication = false;
    private boolean isBrowserOpen = false;
    private boolean uploading = false;
    private Thread thread;

    public SkinLibraryScreen() {
        super(Text.translatable("gui.skin_library.title"));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected void init() {
        super.init();

        SkinCache.clearRequested();

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
        });
    }

    private void reloadDatabase(Runnable callback) {
        // fetch projects
        CompletableFuture.runAsync(() -> {
            Response response = request(Api.HttpMethod.GET, ContentListResponse.class, "content/mca");
            if (response instanceof ContentListResponse contentListResponse) {
                content.clear();
                Collections.addAll(content, contentListResponse.contents());
                updateSearch();

                if (page == Page.LOADING) {
                    setPage(Page.CLOTHING);
                }
            } else {
                setError(Text.translatable("gui.skin_library.list_fetch_failed"));
            }
        }).thenRunAsync(() -> {
            Response response = request(Api.HttpMethod.GET, TagListResponse.class, "tag/mca");
            if (response instanceof TagListResponse tagListResponse) {
                tags.clear();
                tags.addAll(List.of(tagListResponse.tags()));
                filteredTags.clear();
            }
        }).thenRunAsync(callback);
    }

    private void clearSearch() {
        filteredString = "";
        filteredTags.clear();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        switch (page) {
            case CLOTHING, HAIR -> {
                NbtCompound nbt = new NbtCompound();
                villagerVisualization.readCustomDataFromNbt(nbt);
                villagerVisualization.setBreedingAge(0);
                villagerVisualization.calculateDimensions();

                int i = 0;
                for (int y = 0; y < CLOTHES_V; y++) {
                    for (int x = 0; x < CLOTHES_H + y; x++) {
                        int index = selectionPage * CLOTHES_PER_PAGE + i;
                        if (filteredContent.size() > index) {
                            villagerVisualization.limbAngle = System.currentTimeMillis() / 50.0f + i * 17.0f;
                            villagerVisualization.limbDistance = 1.5f;

                            Identifier identifier = SkinCache.getTextureIdentifier(filteredContent.get(index));

                            villagerVisualization.setHair(EMPTY_IDENTIFIER);
                            villagerVisualization.setClothes(identifier);

                            int cx = width / 2 + (int)((x - CLOTHES_H / 2.0 + 0.5 - 0.5 * (y % 2)) * 40);
                            int cy = height / 2 + 25 + (int)((y - CLOTHES_V / 2.0 + 0.5) * 65);

                            InventoryScreen.drawEntity(cx, cy, 30, -(mouseX - cx) / 2.0f, -(mouseY - cy) / 2.0f, villagerVisualization);
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
                    MinecraftClient.getInstance().getTextureManager().registerTexture(CANVAS_IDENTIFIER, new NativeImageBackedTexture(ClientUtils.byteImageToNativeImage(currentImage)));
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

    protected void mouseMoved(double x, double y, double deltaX, double deltaY) {
        if (isPanning) {
            float ox = (float)(deltaX * (x1 - x0) / 64 / CANVAS_SCALE);
            x0 -= ox;
            x1 -= ox;

            float oy = (float)(deltaY * (y1 - y0) / 64 / CANVAS_SCALE);
            y0 -= oy;
            y1 -= oy;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            isPanning = true;
        }

        if (keyCode == GLFW.GLFW_KEY_R) {
            x0 = 0.0f;
            x1 = 1.0f;
            y0 = 0.0f;
            y1 = 1.0f;
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
        if (!isPanning) {
            if (activeMouseButton >= 0) {
                int x = (int)getPixelX();
                int y = (int)getPixelY();
                ClientUtils.bethlehemLine(lastPixelMouseX, lastPixelMouseY, x, y, this::paint);
                lastPixelMouseX = x;
                lastPixelMouseY = y;
            }
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
        if (validPixel(x, y)) {
            if (activeMouseButton == 0) {
                currentImage.setPixel(x, y, color.getRed(), color.getGreen(), color.getBlue(), 255);
                shouldUpload = true;
            } else if (activeMouseButton == 1) {
                currentImage.setPixel(x, y, 0, 0, 0, 0);
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

            int x = width / 2 - 200;
            int w = 400 / b.size();
            for (Page page : b) {
                addDrawableChild(new ButtonWidget(x, height / 2 - 90 - 22, w, 20, Text.translatable("gui.skin_library.page." + page.name().toLowerCase(Locale.ROOT)), sender -> setPage(page))).active = page != this.page;
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
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 200 + 65, height / 2 - 88, 130, 16,
                        Text.translatable("gui.skin_library.search")));
                textFieldWidget.setMaxLength(64);
                textFieldWidget.setSuggestion("search");
                textFieldWidget.setChangedListener(s -> {
                    filteredString = s;
                    updateSearch();
                    textFieldWidget.setSuggestion(null);
                });

                //settings
                addDrawableChild(new TooltipButtonWidget(width / 2 - 200, height / 2 - 90, 60, 20,
                        "gui.skin_library.subscription_filter." + subscriptionFilter.name().toLowerCase(Locale.ROOT),
                        v -> {
                            if (subscriptionFilter == SubscriptionFilter.LIBRARY) {
                                subscriptionFilter = SubscriptionFilter.GLOBAL;
                            } else if (subscriptionFilter == SubscriptionFilter.GLOBAL) {
                                subscriptionFilter = SubscriptionFilter.PLAYER;
                            } else {
                                subscriptionFilter = SubscriptionFilter.LIBRARY;
                            }
                            ((TooltipButtonWidget)v).setMessage("gui.skin_library.subscription_filter." + subscriptionFilter.name().toLowerCase(Locale.ROOT));
                        }));

                //tags
                int tx = width / 2 - 200 + 65 + 135;
                for (String tag : tags) {
                    int w = textRenderer.getWidth(tag) + 10;
                    addDrawableChild(new ButtonWidget(tx, height / 2 - 90, w, 20,
                            Text.literal(tag),
                            v -> {
                            }));
                    tx += w;
                }

                //controls
                int i = 0;
                for (int y = 0; y < CLOTHES_V; y++) {
                    for (int x = 0; x < CLOTHES_H + y; x++) {
                        int index = selectionPage * CLOTHES_PER_PAGE + i;
                        if (filteredContent.size() > index) {
                            int cx = width / 2 + (int)((x - CLOTHES_H / 2.0 + 0.5 - 0.5 * (y % 2)) * 40);
                            int cy = height / 2 + 25 + (int)((y - CLOTHES_V / 2.0 + 0.5) * 65);

                            addDrawableChild(new TooltipButtonWidget(cx, cy, 25, 20,
                                    Text.translatable("+"),
                                    Text.translatable("subscribe"),
                                    v -> {
                                    }));
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
                            setPage(Page.EDITOR);
                            settings.skinType = SkinType.HAIR;
                        }));

                addDrawableChild(new ButtonWidget(width / 2 + 5, height / 2, 95, 20,
                        Text.translatable("gui.skin_library.prepare.clothing"),
                        v -> {
                            setPage(Page.EDITOR);
                            settings.skinType = SkinType.CLOTHING;
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
                //todo tags
                //todo preview
                addDrawableChild(new ButtonWidget(width / 2 + 100, height / 2, 100, 20,
                        Text.translatable("gui.skin_library.cancel"),
                        v -> {
                            setPage(Page.CLOTHING);
                        }));
            }
            case EDITOR -> {
                // name
                TextFieldWidget textFieldWidget = addDrawableChild(new TextFieldWidget(this.textRenderer, width / 2 - 60, height / 2 - 105, 120, 20,
                        Text.translatable("gui.skin_library.name")));
                textFieldWidget.setMaxLength(1024);
                textFieldWidget.setSuggestion("Name");
                textFieldWidget.setText(settings.title);
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
                addDrawableChild(new GeneSliderWidget(width / 2 - 200, height / 2 - 80, 50, 20,
                        Text.translatable("gui.skin_library.chance"),
                        1.0,
                        v -> {
                            settings.chance = v;
                        }));

                // temperature
                addDrawableChild(new IntegerSliderWidget(width / 2 - 145, height / 2 - 80, 50, 20,
                        settings.temperature,
                        -2, 2,
                        v -> {
                            settings.temperature = v;
                        },
                        v -> Text.translatable("gui.skin_library.temperature." + (v + 2))
                ));

                //profession
                int ox = 0;
                int oy = 0;
                List<ItemButtonWidget> widgets = new LinkedList<>();
                for (VillagerProfession profession : Registry.VILLAGER_PROFESSION) {
                    MutableText text = Text.translatable("entity.minecraft.villager." + profession.id());
                    ItemButtonWidget widget = addDrawableChild(new ItemButtonWidget(width / 2 - 200 + ox * 21, height / 2 - 50 + oy * 21, 20, text,
                            Items.LECTERN.getDefaultStack(),
                            v -> {
                                settings.profession = profession != VillagerProfession.NONE ? null : profession.id();
                                widgets.forEach(b -> b.active = false);
                                v.active = true;
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

                //hue              //todo refresh on set color
                addDrawableChild(new HorizontalColorPickerWidget(width / 2 + 100, y, 100, 15,
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
                colorSelector selector = new colorSelector();
                addDrawableChild(new HorizontalGradientWidget(width / 2 + 100, y + 20, 100, 15,
                        color.saturation,
                        () -> {
                            selector.setHSV(color.hue, 0.0, 1.0);
                            return new float[] {
                                    (float)selector.red, (float)selector.green, (float)selector.blue, 1.0f,
                            };
                        },
                        () -> {
                            selector.setHSV(color.hue, 1.0, 1.0);
                            return new float[] {
                                    (float)selector.red, (float)selector.green, (float)selector.blue, 1.0f,
                            };
                        },
                        (vx, vy) -> {
                            color.setHSV(
                                    color.hue,
                                    vx,
                                    color.brightness
                            );
                        }));

                //brightness
                addDrawableChild(new HorizontalGradientWidget(width / 2 + 100, y + 40, 100, 15,
                        color.brightness,
                        () -> {
                            selector.setHSV(color.hue, color.saturation, 0.0);
                            return new float[] {
                                    (float)selector.red, (float)selector.green, (float)selector.blue, 1.0f,
                            };
                        },
                        () -> {
                            selector.setHSV(color.hue, color.saturation, 1.0);
                            return new float[] {
                                    (float)selector.red, (float)selector.green, (float)selector.blue, 1.0f,
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

        rebuild();

        if (page == Page.CLOTHING || page == Page.HAIR) {
            updateSearch();
        }
    }

    private void updateSearch() {
        if (Thread.currentThread() != thread) {
            assert client != null;
            client.executeSync(this::updateSearch);
            return;
        }

        filteredContent.clear();

        filteredContent.addAll((subscriptionFilter == SubscriptionFilter.LIBRARY ? content : serverContent).stream()
                .filter(v -> filteredString.isEmpty() || v.title().contains(filteredString))
                .filter(v -> v.hasTag("clothing"))
                .toList());

        setSelectionPage(selectionPage);
    }

    private String getPlayerName() {
        return MinecraftClient.getInstance().player == null ? "Unknown" : MinecraftClient.getInstance().player.getGameProfile().getName();
    }

    private boolean isOp() {
        return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.hasPermissionLevel(4);
    }

    private void setSelectionPage(int p) {
        selectionPage = Math.max(0, Math.min(getMaxPages() - 1, p));
        pageWidget.setMessage(Text.literal((selectionPage + 1) + " / " + getMaxPages()));
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
                currentImage = ByteImage.read(stream);
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
                    Response request = request(settings.contentid == -1 ? Api.HttpMethod.POST : Api.HttpMethod.PUT, ContentIdResponse.class, settings.contentid == -1 ? "content/mca" : "content/mca/%s".formatted(settings.contentid), Map.of(
                            "token", Auth.getToken()
                    ), Map.of(
                            "title", settings.title,
                            "meta", "{}",
                            "data", new String(Base64.getEncoder().encode(currentImage.encode()))
                    ));

                    if (request instanceof ContentIdResponse response) {
                        reloadDatabase(() -> {
                            int contentid = response.contentid();

                            // default tags
                            setTag(contentid, settings.skinType.name().toLowerCase(Locale.ROOT), true);
                            setTag(contentid, settings.profession == null ? "generic" : settings.profession, true);

                            // open detail page
                            focusedSkin = contentid;
                            setPage(Page.DETAIL);
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

    private void setTag(int contentid, String tag, boolean add) {
        if (Auth.getToken() != null) {
            request(add ? Api.HttpMethod.POST : Api.HttpMethod.DELETE, SuccessResponse.class, "tag/mca/%s/%s".formatted(contentid, tag), Map.of(
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
        PLAYER,
        GLOBAL
    }

    public static final class WorkspaceSettings {
        public int contentid = -1;

        public int temperature;
        public double chance = 1.0;
        public String title = "";
        public String profession;
        public SkinType skinType;

        public WorkspaceSettings() {

        }
    }

    static class colorSelector {
        double red, green, blue;
        double hue, saturation, brightness;

        public colorSelector() {
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
        }

        private void updateRGB() {
            double c = brightness * saturation;
            double x = c * (1 - Math.abs(hue / 60 % 2 - 1));
            double m = brightness - c;
            double r, g, b;

            if (hue >= 0 && hue < 60) {
                r = c;
                g = x;
                b = 0;
            } else if (hue >= 60 && hue < 120) {
                r = x;
                g = c;
                b = 0;
            } else if (hue >= 120 && hue < 180) {
                r = 0;
                g = c;
                b = x;
            } else if (hue >= 180 && hue < 240) {
                r = 0;
                g = x;
                b = c;
            } else if (hue >= 240 && hue < 300) {
                r = x;
                g = 0;
                b = c;
            } else {
                r = c;
                g = 0;
                b = x;
            }

            this.red = r + m;
            this.green = g + m;
            this.blue = b + m;
        }

        private void updateHSV() {
            double cmax = Math.max(red, Math.max(green, blue));
            double cmin = Math.min(red, Math.min(green, blue));
            double delta = cmax - cmin;
            double h, s, v;

            if (delta == 0) {
                h = 0;
            } else if (cmax == red) {
                h = (green - blue) / delta % 6;
            } else if (cmax == green) {
                h = (blue - red) / delta + 2;
            } else {
                h = (red - green) / delta + 4;
            }

            h *= 60;

            if (h < 0) {
                h += 360;
            }

            if (cmax == 0) {
                s = 0;
            } else {
                s = delta / cmax;
            }

            v = cmax;

            this.hue = h;
            this.saturation = s;
            this.brightness = v;
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
    }
}
