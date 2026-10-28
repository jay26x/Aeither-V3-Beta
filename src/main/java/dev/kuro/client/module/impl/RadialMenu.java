package dev.kuro.client.module.impl;

import dev.kuro.client.gui.RadialMenuScreen;
import dev.kuro.client.module.Category;
import dev.kuro.client.module.Module;
import dev.kuro.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Radial Menu (Player): a radial quick-chat menu. Bind a key to it and HOLD that key: a circle opens in the middle of the
 * screen and splits into three slices; let go of the key and it closes again. Each slice holds a text; left click a slice and it is sent for you, as a chat
 * message, or as a command when it starts with "/" (client commands with your prefix, e.g. ".toggle fly", work too).
 * Right click a slice to type its text (spaces are fine). The three texts are saved in config/kuro-radial.txt (the old kuro-circle.txt is still read once if the new file doesn't exist yet).
 */
public class RadialMenu extends Module {
    public static final int SLOTS = 3;

    private final Setting.Bool closeAfter = add(new Setting.Bool("Close after sending", true));
    private final String[] slots = {"", "", ""};
    private int pendingEdit = -1;
    private boolean screenClosing;       // the screen itself is closing: don't close it again from onDisable
    private boolean typing;              // a slice's text is being typed: letting go of the key must not close the menu then

    public RadialMenu() {
        super("Radial Menu", Category.PLAYER);
        load();
        for (int i = 0; i < SLOTS; i++) {
            final int n = i;
            add(new Setting.Button("Edit slot " + (i + 1), () -> openEditor(n)));
        }
        add(new Setting.Button("Clear all slots", () -> { for (int i = 0; i < SLOTS; i++) slots[i] = ""; save(); }));
    }

    /** Hold the key to keep the menu open, release it to close (module key = hold to use). Not while a slice is being typed into. */
    @Override public boolean holdToUse() { return !typing; }

    /** Called by RadialMenuScreen while a slice's text is being typed. */
    public void setTyping(boolean on) { typing = on; }

    public String slot(int i) { return slots[i]; }
    public boolean closeAfterSending() { return closeAfter.value; }

    public void setSlot(int i, String text) {
        slots[i] = clean(text);
        save();
    }

    /** No control characters or section signs (the game rejects those in chat), at most 256 characters. */
    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) if (!Character.isISOControl(c) && c != '§') sb.append(c);
        String t = sb.toString().strip();
        return t.length() > 256 ? t.substring(0, 256) : t;
    }

    /** Sends slot i: a "/command" goes out as a command, everything else as chat (client commands are caught by the command manager). */
    public void send(int i) {
        String t = slots[i];
        ClientPacketListener net = mc.getConnection();
        if (t.isEmpty() || net == null) return;
        if (t.startsWith("/")) { if (t.length() > 1) net.sendCommand(t.substring(1)); }
        else BaritoneControl.sendPlainChat(net, t);   // plain chat must not be swallowed by Baritone's chat control
    }

    private void openEditor(int slot) {
        if (mc.player == null) return;
        pendingEdit = slot;
        if (isEnabled()) { pendingEdit = -1; mc.setScreen(new RadialMenuScreen(this, slot)); }
        else setEnabled(true);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) { setEnabled(false); return; }
        int edit = pendingEdit;
        pendingEdit = -1;
        mc.setScreen(new RadialMenuScreen(this, edit));
    }

    /** Called by RadialMenuScreen#removed: the screen is gone, so switch the module off without touching the screen. */
    public void screenRemoved() {
        typing = false;
        screenClosing = true;
        try { setEnabled(false); } finally { screenClosing = false; }
    }

    @Override
    protected void onDisable() {
        if (!screenClosing && mc.screen instanceof RadialMenuScreen) mc.setScreen(null);
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("kuro-radial.txt"); }
    private static Path oldFile() { return FabricLoader.getInstance().getConfigDir().resolve("kuro-circle.txt"); }

    private void load() {
        try {
            Path p = file();
            if (!Files.exists(p)) p = oldFile();      // slots saved before the rename
            if (!Files.exists(p)) return;
            List<String> lines = Files.readAllLines(p);
            for (int i = 0; i < SLOTS && i < lines.size(); i++) slots[i] = clean(lines.get(i));
        } catch (Exception ignored) {}
    }

    private void save() {
        try { Files.writeString(file(), String.join("\n", slots)); } catch (Exception ignored) {}
    }
}
