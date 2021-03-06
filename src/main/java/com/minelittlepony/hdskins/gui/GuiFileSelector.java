package com.minelittlepony.hdskins.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.ScrollContainer;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.packing.GridPacker;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.hdskins.HDConfig;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.upload.IFileDialog;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class GuiFileSelector extends GameGui implements IFileDialog {

    protected Path currentDirectory = Paths.get("/");

    private IFileDialog.Callback callback = (f, b) -> {};

    private final GridPacker packer = new GridPacker()
            .setItemWidth(150)
            .setItemHeight(20);

    private Button parentBtn;

    protected TextFieldWidget textInput;

    protected final ScrollContainer filesList = new ScrollContainer();

    private String extensionFilter = "";
    private String filterMessage = "";

    public GuiFileSelector(String title) {
        super(new LiteralText(title));

        filesList.margin.top = 60;
        filesList.margin.bottom = 30;

        filesList.padding.setAll(10);

        currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();
    }

    @Override
    protected void init() {
        children.add(filesList);

        renderDirectory();

        addButton(textInput = new TextFieldWidget(font, 10, 30, width - 50, 18, ""));
        textInput.setIsEditable(true);
        textInput.setMaxLength(Integer.MAX_VALUE);
        textInput.setText(currentDirectory.toAbsolutePath().toString());
        addButton(new Button(width - 30, 29, 20, 20))
            .onClick(p -> navigateTo(Paths.get(textInput.getText())))
            .getStyle()
                .setText("hdskins.directory.go");

        addButton(new Label(width/2, 5).setCentered())
            .getStyle()
            .setText(getTitle().getString());

        addButton(parentBtn = new Button(width/2 - 160, height - 25, 100, 20))
            .onClick(p -> navigateTo(currentDirectory.getParent()))
            .setEnabled(currentDirectory.getParent() != null)
            .getStyle()
                .setText("hdskins.directory.up");

        addButton(new Button(width/2 + 60, height - 25, 100, 20))
            .onClick(p -> {
                finish();
                callback.onDialogClosed(currentDirectory, false);
            })
            .getStyle()
                .setText("hdskins.options.close");

        if (!filterMessage.isEmpty()) {
            filesList.margin.bottom = 60;

            addButton(new Label(10, height - 55))
                .getStyle()
                    .setColor(0x88EEEEEE)
                    .setText("* " + filterMessage);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground(0);
        super.render(mouseX, mouseY, partialTicks);

        filesList.render(mouseX, mouseY, partialTicks);
    }

    protected void renderDirectory() {
        filesList.init(() -> {
            int buttonX = filesList.width / 2 - 110;

            listFiles().forEach(path -> {
                int buttonY = filesList.buttons().size() * 20;

                filesList.addButton(new PathButton(buttonX, buttonY, 150, 20, path));
            });

            packer.setListWidth(width).pack(filesList);
        });
    }

    protected Stream<Path> listFiles() {
        Path directory = currentDirectory;

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
        }

        File[] files = null;
        try {
            files = directory.toFile().listFiles();
        } catch (Throwable e) {}

        if (files == null) {
            return Stream.empty();
        }

        return Lists.newArrayList(files).stream()
                .map(File::toPath)
                .filter(this::filterPath);
    }

    protected boolean filterPath(Path path) {
        try {
            if (Files.isHidden(path)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return extensionFilter.isEmpty()
                || Files.isDirectory(path)
                || path.getFileName().toString().endsWith(extensionFilter);
    }

    public void navigateTo(Path path) {
        if (path == null) {
            return;
        }

        path = path.toAbsolutePath();

        textInput.setText(path.toString());

        HDConfig config = HDSkins.getInstance().getConfig();

        if (Files.isDirectory(path)) {
            currentDirectory = path;

            config.lastChosenFile.set(path);

            parentBtn.setEnabled(currentDirectory.getParent() != null);
            renderDirectory();
        } else {
            config.lastChosenFile.set(path.getParent());
            onFileSelected(path);
        }

        config.save();
    }

    protected void onFileSelected(Path fileLocation) {
        minecraft.openScreen(parent);
        callback.onDialogClosed(fileLocation, true);
    }

    protected void onPathSelected(PathButton sender) {
        navigateTo(sender.path);
    }

    private static final Identifier ICONS = new Identifier("hdskins", "textures/gui/files.png");

    class PathButton extends Button {

        protected final Path path;

        public PathButton(int x, int y, int width, int height, Path path) {
            super(x, y, width, height);

            this.path = path;

            String name = path.getFileName().toString();

            TextureSprite sprite = new TextureSprite()
                    .setPosition(6, 6)
                    .setTexture(ICONS)
                    .setTextureSize(22, 22)
                    .setSize(10, 8);

            if (!Files.isDirectory(path)) {
                sprite.setTextureOffset(0, 14);

                String[] splitten = path.toString().split("\\.");
                String ext = splitten[splitten.length - 1];
                if (ext.matches("png|jpg|bmp")) {
                    sprite.setTextureOffset(13, 0);
                }
            }

            onClick(self -> onPathSelected(this));
            setEnabled(Files.isReadable(path));
            getStyle()
                .setText(minecraft.textRenderer.trimToWidth(name, width - 70))
                .setIcon(sprite)
                .setTooltip(Lists.newArrayList(
                        name,
                        Formatting.GRAY + "" + Formatting.ITALIC + describeFile(path))
                );
        }

        public void clearFocus() {
            super.setFocused(false);
        }

        protected String describeFile(Path path) {
            if (Files.isDirectory(path)) {
                return I18n.translate("hdskins.filetype.directory");
            }

            String[] split = path.getFileName().toString().split("\\.");
            if (split.length > 1) {
                return I18n.translate("hdskins.filetype.file", split[1].toUpperCase());
            }

            return I18n.translate("hdskins.filetype.unknown");
        }
    }

    @Override
    public IFileDialog filter(String extension, String description) {
        extensionFilter = Strings.nullToEmpty(extension);
        filterMessage = Strings.nullToEmpty(description);

        if (!filterMessage.isEmpty()) {
            filesList.margin.bottom = 60;
        } else {
            filesList.margin.bottom = 30;
        }
        return this;
    }

    @Override
    public IFileDialog andThen(Callback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public IFileDialog launch() {
        MinecraftClient.getInstance().openScreen(this);
        return this;
    }
}
