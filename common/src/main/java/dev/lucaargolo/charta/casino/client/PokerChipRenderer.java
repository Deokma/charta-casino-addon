package dev.lucaargolo.charta.casino.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lucaargolo.charta.client.render.block.CardTableBlockEntityRenderer;
import dev.lucaargolo.charta.common.block.entity.CardTableBlockEntity;
import dev.lucaargolo.charta.common.game.api.GameSlot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class PokerChipRenderer {

    private PokerChipRenderer() {}

    private static final RenderType CHIP_RT = RenderType.gui();

    /**
     * Number of polygon segments — intentionally low (8) to get the
     * pixelated / blocky Minecraft look with clearly visible flat faces.
     */
    private static final int SEGS = 8;

    private static final int[][] CHIP_COLORS = {
            { 0xFFE8E8E8, 0xFFAAAAAA }, // 0  white
            { 0xFFEE3333, 0xFFAA1111 }, // 1  red
            { 0xFF33BB33, 0xFF118811 }, // 2  green
            { 0xFF555555, 0xFF333333 }, // 3  black
            { 0xFF9933EE, 0xFF6611BB }, // 4  purple
            { 0xFF3366EE, 0xFF1144AA }, // 5  blue
    };
    private static final int[] CHIP_ALLIN  = { 0xFFFFDD00, 0xFFCC8800 };
    private static final int[] CHIP_FOLDED = { 0xFF888888, 0xFF555555 };

    // Pre-computed unit circle vertices [SEGS + 1]
    private static final float[] COS_TABLE = new float[SEGS + 1];
    private static final float[] SIN_TABLE = new float[SEGS + 1];

    static {
        for (int i = 0; i <= SEGS; i++) {
            double a = 2.0 * Math.PI * i / SEGS;
            COS_TABLE[i] = (float) Math.cos(a);
            SIN_TABLE[i] = (float) Math.sin(a);
        }
    }

    public static void register() {
        CardTableBlockEntityRenderer.EXTRA_RENDERER = PokerChipRenderer::render;
    }

    // -------------------------------------------------------------------------

    private static void render(CardTableBlockEntity blockEntity, float partialTick,
                               PoseStack poseStack, MultiBufferSource bufferSource,
                               int packedLight, int packedOverlay) {

        BlockPos tablePos = blockEntity.getBlockPos();
        int[]   chips = CasinoClientData.TABLE_POKER_CHIPS.get(tablePos);
        Integer gameSlotCountObj = CasinoClientData.TABLE_POKER_GAME_SLOT_COUNT.get(tablePos);
        if (chips == null || gameSlotCountObj == null) return;

        int gameSlotCount = gameSlotCountObj;
        int gameSlots     = blockEntity.getSlotCount();
        int foldedMask    = CasinoClientData.TABLE_POKER_FOLDED.getOrDefault(tablePos, 0);
        int allInMask     = CasinoClientData.TABLE_POKER_ALLIN.getOrDefault(tablePos, 0);
        int startingChips = CasinoClientData.TABLE_POKER_STARTING_CHIPS.getOrDefault(tablePos, 1000);
        if (startingChips <= 0) startingChips = 1000;

        final int[]   STACK_COLORS   = { 1, 5, 2 };
        final float[] SIDE_OFF       = { -11f, 11f, 0f };
        final float[] SIDE_OFF_DEPTH = { -5f,  5f,  0f };
        final float[] FRACTIONS      = { 1.0f, 0.85f, 0.70f };
        final int     MAX_DISCS      = 12;

        for (int pi = 0; pi < chips.length; pi++) {
            int handSlotIndex = gameSlotCount + pi;
            if (handSlotIndex >= gameSlots) break;

            int     totalChips = chips[pi];
            boolean isFolded   = (foldedMask & (1 << pi)) != 0;
            boolean isAllIn    = (allInMask  & (1 << pi)) != 0;
            if (totalChips <= 0) continue;

            float pct       = (float) totalChips / startingChips;
            int   baseDiscs = Math.max(1, Math.round(pct * MAX_DISCS));

            GameSlot handSlot = blockEntity.getSlot(handSlotIndex);
            float hx = handSlot.lerpX(partialTick);
            float hy = handSlot.lerpY(partialTick);

            final float HAND_TO_EDGE = 147.5f;
            final float DEPTH_INSET  = 2f;

            float tableCX = 80f + blockEntity.centerOffset.x * 160f;
            float tableCY = 80f + blockEntity.centerOffset.y * 160f;

            float dx    = tableCX - hx;
            float dy    = tableCY - hy;
            float dist  = (float) Math.sqrt(dx * dx + dy * dy);
            float nx    = dx / dist;
            float ny    = dy / dist;
            float sideX = -ny;
            float sideY =  nx;

            float anchorX = hx + nx * (HAND_TO_EDGE + DEPTH_INSET);
            float anchorY = hy + ny * (HAND_TO_EDGE + DEPTH_INSET);

            for (int s = 0; s < STACK_COLORS.length; s++) {
                int   discs = Math.max(1, Math.round(baseDiscs * FRACTIONS[s]));
                float cx    = anchorX + sideX * SIDE_OFF[s];
                float cy    = anchorY + sideY * SIDE_OFF[s];
                if (Math.abs(sideX) >= Math.abs(sideY)) {
                    cy += SIDE_OFF_DEPTH[s];
                } else {
                    cx += SIDE_OFF_DEPTH[s];
                }
                drawChipStack(poseStack, bufferSource, cx, cy,
                        discs, isFolded, isAllIn, STACK_COLORS[s]);
            }
        }
    }

    // -------------------------------------------------------------------------

    private static void drawChipStack(PoseStack poseStack, MultiBufferSource bufferSource,
                                      float px, float py, int numDiscs,
                                      boolean folded, boolean allIn, int colorIndex) {
        if (numDiscs <= 0) return;

        int[] pair = folded ? CHIP_FOLDED
                   : allIn  ? CHIP_ALLIN
                   : CHIP_COLORS[Mth.clamp(colorIndex, 0, CHIP_COLORS.length - 1)];

        final float DISC_R   = 7.5f;   // radius of the circular chip
        final float DISC_H   = 2.5f;   // height of one disc
        final float DISC_GAP = 0.0f;
        final float STRIPE_H = 0.55f;  // white rim stripe height
        final float STEP     = DISC_H + DISC_GAP;

        // Edge-spots on the top face — 4 spots aligned with the flat sides of the octagon
        final int   NUM_SPOTS  = 4;
        final float SPOT_DIST  = DISC_R * 0.62f;
        final float SPOT_RW    = 1.4f;
        final float SPOT_RH    = 2.4f;

        float topR  = ch_r(pair[0]), topG  = ch_g(pair[0]), topB  = ch_b(pair[0]);
        float sideR = ch_r(pair[1]), sideG = ch_g(pair[1]), sideB = ch_b(pair[1]);

        poseStack.pushPose();
        poseStack.scale(1f / 160f, 1f / 160f, 1f / 160f);

        PoseStack.Pose e  = poseStack.last();
        VertexConsumer vc = bufferSource.getBuffer(CHIP_RT);

        for (int d = 0; d < numDiscs; d++) {
            float zb    = d * STEP;
            float zt    = zb + DISC_H;
            float ztRim = zt - STRIPE_H;

            // Alternate brightness so adjacent discs are easy to count
            float alt = (d % 2 == 0) ? 1.0f : 0.86f;

            // ---- Bottom face (only the first disc) ----
            if (d == 0) {
                drawCircle(e, vc, px, py, zb, DISC_R,
                        sideR * 0.60f, sideG * 0.60f, sideB * 0.60f);
            }

            // ---- Main cylindrical side ----
            drawCylinderSides(e, vc, px, py, zb, ztRim, DISC_R,
                    sideR * alt, sideG * alt, sideB * alt);

            // ---- White rim stripe (top band of the cylinder) ----
            drawCylinderSides(e, vc, px, py, ztRim, zt, DISC_R,
                    0.92f * alt, 0.90f * alt, 0.88f * alt);

            // ---- Top face ----
            drawCircle(e, vc, px, py, zt, DISC_R,
                    topR * alt, topG * alt, topB * alt);

            // ---- Edge spots on the top face ----
            final float ZR   = zt + 0.02f;
            float rectR = Math.min(1f, topR * alt + 0.32f);
            float rectG = Math.min(1f, topG * alt + 0.32f);
            float rectB = Math.min(1f, topB * alt + 0.32f);

            for (int sp = 0; sp < NUM_SPOTS; sp++) {
                double angle = 2.0 * Math.PI * sp / NUM_SPOTS;
                float  ca    = (float) Math.cos(angle);   // radial direction
                float  sa    = (float) Math.sin(angle);
                float  ta    = -sa;                        // tangential direction
                float  tb    =  ca;

                float spx = px + SPOT_DIST * ca;
                float spy = py + SPOT_DIST * sa;

                // Four corners of the rotated rectangle
                float c0x = spx + (-SPOT_RW * ta - SPOT_RH * ca);
                float c0y = spy + (-SPOT_RW * tb - SPOT_RH * sa);
                float c1x = spx + ( SPOT_RW * ta - SPOT_RH * ca);
                float c1y = spy + ( SPOT_RW * tb - SPOT_RH * sa);
                float c2x = spx + ( SPOT_RW * ta + SPOT_RH * ca);
                float c2y = spy + ( SPOT_RW * tb + SPOT_RH * sa);
                float c3x = spx + (-SPOT_RW * ta + SPOT_RH * ca);
                float c3y = spy + (-SPOT_RW * tb + SPOT_RH * sa);

                quad(e, vc,
                        c0x, c0y, ZR,
                        c1x, c1y, ZR,
                        c2x, c2y, ZR,
                        c3x, c3y, ZR,
                        rectR, rectG, rectB);
            }
        }

        poseStack.popPose();
    }

    // -------------------------------------------------------------------------
    // Geometry helpers — circular / cylindrical
    // -------------------------------------------------------------------------

    /**
     * Filled circle as a fan of degenerate quads (center + two edge points).
     * Each "quad" has vertices: (center, edge[i], edge[i+1], edge[i+1]),
     * which produces one valid triangle and one zero-area degenerate triangle.
     */
    private static void drawCircle(PoseStack.Pose e, VertexConsumer vc,
                                   float cx, float cy, float z, float r,
                                   float red, float grn, float blu) {
        for (int i = 0; i < SEGS; i++) {
            float x0 = cx + r * COS_TABLE[i],     y0 = cy + r * SIN_TABLE[i];
            float x1 = cx + r * COS_TABLE[i + 1], y1 = cy + r * SIN_TABLE[i + 1];
            // Fan triangle as degenerate quad
            quad(e, vc,
                    cx, cy, z,
                    x0, y0, z,
                    x1, y1, z,
                    x1, y1, z,   // repeated → degenerate
                    red, grn, blu);
        }
    }

    /**
     * Cylindrical side surface — one quad per segment, with per-face diffuse
     * shading based on the outward normal of each face vs. the light direction (+X, −Y)/√2.
     */
    private static void drawCylinderSides(PoseStack.Pose e, VertexConsumer vc,
                                          float cx, float cy,
                                          float zb, float zt,
                                          float r,
                                          float red, float grn, float blu) {
        for (int i = 0; i < SEGS; i++) {
            float x0 = cx + r * COS_TABLE[i],     y0 = cy + r * SIN_TABLE[i];
            float x1 = cx + r * COS_TABLE[i + 1], y1 = cy + r * SIN_TABLE[i + 1];

            // Face normal ≈ average of the two edge normals (already unit length for flat face)
            float nx  = (COS_TABLE[i] + COS_TABLE[i + 1]) * 0.5f;
            float ny  = (SIN_TABLE[i] + SIN_TABLE[i + 1]) * 0.5f;
            // Dot with light direction (+X, −Y)/√2
            float dot = nx * 0.7071f + ny * (-0.7071f);
            float sh  = 0.40f + 0.60f * ((dot + 1f) * 0.5f);

            quad(e, vc,
                    x0, y0, zb,
                    x1, y1, zb,
                    x1, y1, zt,
                    x0, y0, zt,
                    red * sh, grn * sh, blu * sh);
        }
    }

    // -------------------------------------------------------------------------
    // Low-level primitive
    // -------------------------------------------------------------------------

    private static void quad(PoseStack.Pose e, VertexConsumer vc,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b) {
        vc.addVertex(e.pose(), x0, y0, z0).setColor(r, g, b, 1f);
        vc.addVertex(e.pose(), x1, y1, z1).setColor(r, g, b, 1f);
        vc.addVertex(e.pose(), x2, y2, z2).setColor(r, g, b, 1f);
        vc.addVertex(e.pose(), x3, y3, z3).setColor(r, g, b, 1f);
    }

    private static float ch_r(int c) { return ((c >> 16) & 0xFF) / 255f; }
    private static float ch_g(int c) { return ((c >>  8) & 0xFF) / 255f; }
    private static float ch_b(int c) { return ( c        & 0xFF) / 255f; }
}