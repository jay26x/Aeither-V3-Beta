package dev.kuro.client.module.impl;

import dev.kuro.client.module.Category;
import dev.kuro.client.module.Module;
import dev.kuro.client.setting.Setting;
import net.minecraft.util.Mth;

/**
 * Replaces the sky with a flat colour (white by default, gray, or any tint from the colour strip) and makes the
 * clouds white. The actual colour swap happens in SkyRenderingMixin / CloudRendererMixin / FogRendererMixin,
 * which ask this module for the colours.
 */
public class SkyModule extends Module {
    private static SkyModule instance;

    private final Setting.Bool gray = add(new Setting.Bool("Gray sky", false));
    private final Setting.Num grayLevel = add(new Setting.Num("Gray brightness %", 55, 15, 95, 5));
    private final Setting.Hue tint = add(new Setting.Hue("Sky color", 0.6f));
    private final Setting.Num tintAmount = add(new Setting.Num("Sky color amount %", 0, 0, 100, 5));   // 0 = plain white / gray
    private final Setting.Bool whiteClouds = add(new Setting.Bool("White clouds", true));
    private final Setting.Bool fog = add(new Setting.Bool("Match fog", true));
    private final Setting.Bool glow = add(new Setting.Bool("Hide sunset glow", true));

    public SkyModule() {
        super("Sky", Category.RENDER);
        instance = this;
    }

    public static SkyModule get() { return instance; }

    public static boolean active() { return instance != null && instance.isEnabled(); }

    /** Sky colour as 0xRRGGBB: white (or gray), optionally tinted with the chosen hue. */
    public int skyRgb() {
        float v = gray.value ? (float) (grayLevel.value / 100.0) : 1f;
        float s = (float) (tintAmount.value / 100.0);
        return Mth.hsvToRgb(tint.hue, s, v) & 0xFFFFFF;
    }

    public boolean cloudsOn() { return whiteClouds.value; }
    public boolean fogOn() { return fog.value; }
    public boolean noGlow() { return glow.value; }
}
