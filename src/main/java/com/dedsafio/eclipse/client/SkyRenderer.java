package com.dedsafio.eclipse.client;

import com.dedsafio.eclipse.eclipse.EclipseSky;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Dibuja el cielo del eclipse sin texturas (todo geometría con degradados, así que se ve nítido a cualquier resolución):
 * cúpula degradada, estrellas, sol con corona/rayos y una luna oscura que se acerca.
 */
public final class SkyRenderer {
    private SkyRenderer() {}

    private static final float R = 100.0F;
    private static final int SEG = 40;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int STAR_COUNT = 1600;
    private static float[] stars; // x, y, z, tamaño, brillo

    public static void render(PoseStack ps, float p) {
        Matrix4f m = ps.last().pose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        dome(m, p);
        stars(m, p);

        Vec3 sun = EclipseSky.sunDir();
        Vec3 moon = EclipseSky.moonDir(p);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        glow(m, sun, p);
        RenderSystem.defaultBlendFunc();

        disc(m, sun, 7.0F, 1.0F, 0.98F, 0.92F, 1.0F);
        float moonAlpha = EclipseSky.smooth(0f, 0.08f, p); // la luna "aparece"
        if (moonAlpha > 0f) disc(m, moon, 7.4F, 0.02F, 0.02F, 0.03F, moonAlpha);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    // ---------- helpers ----------

    private static void v(BufferBuilder b, Matrix4f m, Vec3 pt, float r, float g, float bl, float a) {
        b.vertex(m, (float) pt.x, (float) pt.y, (float) pt.z).color(r, g, bl, a).endVertex();
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
    }

    private static Vec3 onSphere(double elevDeg, double az) {
        return EclipseSky.dir(Math.toRadians(elevDeg), az).scale(R);
    }

    // ---------- cúpula ----------

    private static void dome(Matrix4f m, float p) {
        float[] zen = EclipseSky.zenithColor(p);
        float[] hor = EclipseSky.horizonColor(p);
        double[] el = {-90, -5, 0, 10, 25, 45, 70, 90};
        float[] t = {0f, 0f, 0f, 0.20f, 0.45f, 0.70f, 0.92f, 1f};

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < el.length - 1; i++) {
            float[] c0 = mix(hor, zen, t[i]);
            float[] c1 = mix(hor, zen, t[i + 1]);
            for (int s = 0; s < SEG; s++) {
                double a0 = Math.PI * 2.0 * s / SEG;
                double a1 = Math.PI * 2.0 * (s + 1) / SEG;
                v(b, m, onSphere(el[i], a0), c0[0], c0[1], c0[2], 1f);
                v(b, m, onSphere(el[i], a1), c0[0], c0[1], c0[2], 1f);
                v(b, m, onSphere(el[i + 1], a1), c1[0], c1[1], c1[2], 1f);
                v(b, m, onSphere(el[i + 1], a0), c1[0], c1[1], c1[2], 1f);
            }
        }
        tess.end();
    }

    // ---------- estrellas ----------

    private static void stars(Matrix4f m, float p) {
        float amount = EclipseSky.starAmount(p);
        if (amount <= 0.01f) return;
        if (stars == null) {
            Random r = new Random(42L);
            stars = new float[STAR_COUNT * 5];
            for (int i = 0; i < STAR_COUNT; i++) {
                double y = r.nextDouble() * 1.1 - 0.1;
                double a = r.nextDouble() * Math.PI * 2.0;
                double rad = Math.sqrt(Math.max(0.0, 1.0 - y * y));
                stars[i * 5] = (float) (rad * Math.cos(a));
                stars[i * 5 + 1] = (float) y;
                stars[i * 5 + 2] = (float) (rad * Math.sin(a));
                stars[i * 5 + 3] = 0.35f + r.nextFloat() * 0.6f;
                stars[i * 5 + 4] = 0.45f + r.nextFloat() * 0.55f;
            }
        }
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < STAR_COUNT; i++) {
            Vec3 d = new Vec3(stars[i * 5], stars[i * 5 + 1], stars[i * 5 + 2]);
            float size = stars[i * 5 + 3];
            float a = amount * stars[i * 5 + 4];
            Vec3 u = Math.abs(d.y) > 0.98 ? new Vec3(1, 0, 0) : d.cross(UP).normalize();
            Vec3 w = u.cross(d).normalize();
            Vec3 c = d.scale(R);
            Vec3 us = u.scale(size), ws = w.scale(size);
            v(b, m, c.subtract(us).subtract(ws), 1f, 1f, 1f, a);
            v(b, m, c.add(us).subtract(ws), 1f, 1f, 1f, a);
            v(b, m, c.add(us).add(ws), 1f, 1f, 1f, a);
            v(b, m, c.subtract(us).add(ws), 1f, 1f, 1f, a);
        }
        tess.end();
    }

    // ---------- sol: resplandor, corona y rayos (mezcla aditiva) ----------

    private static void glow(Matrix4f m, Vec3 dir, float p) {
        float k = EclipseSky.smooth(0.15f, 1f, p);
        radial(m, dir, new float[]{0f, 6f, 14f, 28f, Mth.lerp(k, 36f, 70f)},
                new float[]{0.95f, 0.70f, 0.28f, 0.08f, 0f}, 1f, 0.97f, 0.90f);

        float c = EclipseSky.smooth(0.7f, 1f, p); // corona del eclipse total
        if (c > 0f) {
            radial(m, dir, new float[]{7f, 9f, 14f, 26f},
                    new float[]{0.90f * c, 0.50f * c, 0.15f * c, 0f}, 1f, 1f, 1f);
        }
        rays(m, dir, k);
    }

    private static void radial(Matrix4f m, Vec3 dir, float[] radii, float[] alphas, float cr, float cg, float cb) {
        Vec3 c = dir.scale(R);
        Vec3 u = dir.cross(UP).normalize();
        Vec3 w = u.cross(dir).normalize();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < radii.length - 1; i++) {
            for (int s = 0; s < SEG; s++) {
                double a0 = Math.PI * 2.0 * s / SEG;
                double a1 = Math.PI * 2.0 * (s + 1) / SEG;
                v(b, m, ringPt(c, u, w, radii[i], a0), cr, cg, cb, alphas[i]);
                v(b, m, ringPt(c, u, w, radii[i], a1), cr, cg, cb, alphas[i]);
                v(b, m, ringPt(c, u, w, radii[i + 1], a1), cr, cg, cb, alphas[i + 1]);
                v(b, m, ringPt(c, u, w, radii[i + 1], a0), cr, cg, cb, alphas[i + 1]);
            }
        }
        tess.end();
    }

    private static Vec3 ringPt(Vec3 c, Vec3 u, Vec3 w, float rad, double a) {
        return c.add(u.scale(Math.cos(a) * rad)).add(w.scale(Math.sin(a) * rad));
    }

    private static void rays(Matrix4f m, Vec3 dir, float k) {
        Vec3 c = dir.scale(R);
        Vec3 u = dir.cross(UP).normalize();
        Vec3 w = u.cross(dir).normalize();
        float len = Mth.lerp(k, 45f, 120f);
        float a = 0.25f + 0.35f * k;
        int n = 12;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            double ang = Math.PI * 2.0 * i / n + 0.2;
            float l = (i % 3 == 0) ? len : len * 0.5f;
            Vec3 d = u.scale(Math.cos(ang)).add(w.scale(Math.sin(ang)));
            Vec3 perp = u.scale(-Math.sin(ang)).add(w.scale(Math.cos(ang)));
            Vec3 mid = c.add(d.scale(l * 0.10));
            v(b, m, c, 1f, 0.98f, 0.92f, a);
            v(b, m, mid.add(perp), 1f, 0.98f, 0.92f, a * 0.8f);
            v(b, m, c.add(d.scale(l)), 1f, 0.98f, 0.92f, 0f);
            v(b, m, mid.subtract(perp), 1f, 0.98f, 0.92f, a * 0.8f);
        }
        tess.end();
    }

    private static void disc(Matrix4f m, Vec3 dir, float radius, float r, float g, float bl, float a) {
        Vec3 c = dir.scale(R);
        Vec3 u = dir.cross(UP).normalize();
        Vec3 w = u.cross(dir).normalize();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        v(b, m, c, r, g, bl, a);
        int seg = 48;
        for (int s = 0; s <= seg; s++) {
            v(b, m, ringPt(c, u, w, radius, Math.PI * 2.0 * s / seg), r, g, bl, a);
        }
        tess.end();
    }
}
