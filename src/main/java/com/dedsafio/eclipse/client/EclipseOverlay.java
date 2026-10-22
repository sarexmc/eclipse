package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.eclipse.EclipseSky;
import com.dedsafio.eclipse.item.LinternaItem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;

/**
 * Oscuridad del eclipse: capa gris que crece (se va "desteñiendo") y luego negro casi total.
 * Con la linterna encendida se abre un haz de luz suave en el centro de la pantalla.
 */
public final class EclipseOverlay {
    private EclipseOverlay() {}

    private static final int SEG = 48;

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!ClientEclipse.isActiveHere()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float p = ClientEclipse.get(partialTick);
        float gray = EclipseSky.grayAmount(p);
        float dark = EclipseSky.darkAmount(p);
        float alpha = Mth.lerp(dark, 0.38f * gray, 0.95f);
        float col = Mth.lerp(dark, 0.45f, 0.0f);
        if (alpha <= 0.001f) return;

        if (!LinternaItem.isHolding(player)) {
            int a = (int) (alpha * 255f);
            int c = (int) (col * 255f);
            g.fill(0, 0, w, h, (a << 24) | (c << 16) | (c << 8) | c);
            return;
        }

        g.flush();
        Matrix4f m = g.pose().last().pose();
        float cx = w / 2f, cy = h / 2f;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // capa oscura con un agujero suave (el haz de la linterna)
        float r0 = h * 0.16f;
        float r1 = h * 0.62f;
        float big = (float) Math.hypot(w, h);
        int steps = 8;
        float[] radii = new float[steps + 3];
        float[] alphas = new float[steps + 3];
        radii[0] = 0f;      alphas[0] = 0f;
        radii[1] = r0;      alphas[1] = 0f;
        for (int i = 1; i <= steps; i++) {
            float r = Mth.lerp(i / (float) steps, r0, r1);
            radii[i + 1] = r;
            alphas[i + 1] = alpha * EclipseSky.smooth(r0, r1, r);
        }
        radii[steps + 2] = big;
        alphas[steps + 2] = alpha;
        ring(m, cx, cy, radii, alphas, col, col, col);

        // brillo cálido dentro del haz (mezcla aditiva)
        float lit = 0.25f + 0.75f * dark;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        ring(m, cx, cy,
                new float[]{0f, r1 * 0.15f, r1 * 0.40f, r1 * 0.75f},
                new float[]{0.30f * lit, 0.20f * lit, 0.07f * lit, 0f},
                1f, 0.97f, 0.88f);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void ring(Matrix4f m, float cx, float cy, float[] radii, float[] alphas, float cr, float cg, float cb) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < radii.length - 1; i++) {
            for (int s = 0; s < SEG; s++) {
                float a0 = (float) (Math.PI * 2.0 * s / SEG);
                float a1 = (float) (Math.PI * 2.0 * (s + 1) / SEG);
                vtx(b, m, cx, cy, radii[i], a0, cr, cg, cb, alphas[i]);
                vtx(b, m, cx, cy, radii[i], a1, cr, cg, cb, alphas[i]);
                vtx(b, m, cx, cy, radii[i + 1], a1, cr, cg, cb, alphas[i + 1]);
                vtx(b, m, cx, cy, radii[i + 1], a0, cr, cg, cb, alphas[i + 1]);
            }
        }
        tess.end();
    }

    private static void vtx(BufferBuilder b, Matrix4f m, float cx, float cy, float r, float ang,
                            float cr, float cg, float cb, float a) {
        b.vertex(m, cx + Mth.cos(ang) * r, cy + Mth.sin(ang) * r, 0f).color(cr, cg, cb, a).endVertex();
    }
}
