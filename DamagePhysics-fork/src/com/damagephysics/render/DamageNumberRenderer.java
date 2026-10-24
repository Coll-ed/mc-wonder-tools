package com.damagephysics.render;

import com.damagephysics.config.DamagePhysicsConfig;
import com.damagephysics.particle.ImpactSparkManager;
import com.damagephysics.physics.DamageNumberBody;
import com.damagephysics.physics.DamagePhysicsWorld;
import com.damagephysics.physics.Vec2;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent.Post;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DamageNumberRenderer {
   private static final DamageNumberRenderer INSTANCE = new DamageNumberRenderer();
   private final Vec2 interpolatedPos = new Vec2();
   /** 世界锚点投影结果（与 {@link #interpolatedPos} 分开，避免覆盖物理插值结果）。 */
   private final Vec2 projectedAnchor = new Vec2();
   /** {@link #projectToScreen} 最近一次算出的相机空间深度（方块），供 3D 缩放使用。 */
   private double lastDepth = 0.0;
   private final Vector3f tempCamSpace = new Vector3f();

   public static DamageNumberRenderer getInstance() {
      return INSTANCE;
   }

   private DamageNumberRenderer() {
   }

   public Vec2 projectToScreen(Vec3 worldPos, Vec2 out) {
      Minecraft mc = Minecraft.getInstance();
      Camera camera = mc.gameRenderer.getMainCamera();
      if (camera == null) {
         return null;
      } else {
         Vec3 camPos = camera.getPosition();
         double dx = worldPos.x - camPos.x;
         double dy = worldPos.y - camPos.y;
         double dz = worldPos.z - camPos.z;
         Quaternionf rotation = camera.rotation();
         this.tempCamSpace.set((float)dx, (float)dy, (float)dz);
         Quaternionf invRot = new Quaternionf(rotation).conjugate();
         invRot.transform(this.tempCamSpace);
         float depth = -this.tempCamSpace.z;
         if (depth <= 0.1F) {
            return null;
         } else {
            this.lastDepth = (double)depth;
            double fovDegrees = (double)((Integer)mc.options.fov().get()).intValue();
            float halfFovTan = (float)Math.tan(Math.toRadians(fovDegrees) * 0.5);
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            float aspectRatio = (float)mc.getWindow().getWidth() / (float)Math.max(1, mc.getWindow().getHeight());
            float ndcX = this.tempCamSpace.x / (depth * halfFovTan * aspectRatio);
            float ndcY = this.tempCamSpace.y / (depth * halfFovTan);
            float guiX = (ndcX + 1.0F) * 0.5F * (float)screenWidth;
            float guiY = (1.0F - ndcY) * 0.5F * (float)screenHeight;
            return out == null ? new Vec2(guiX, guiY) : out.set(guiX, guiY);
         }
      }
   }

   @SubscribeEvent
   public void onRenderGui(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui) {
         DamagePhysicsWorld world = DamagePhysicsWorld.getInstance();
         List<DamageNumberBody> bodies = world.getActiveBodies();
         GuiGraphics guiGraphics = event.getGuiGraphics();
         float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
         Font font = mc.font;
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         ImpactSparkManager.getInstance().renderScreen(guiGraphics);
         if (!bodies.isEmpty()) {
            PoseStack poseStack = guiGraphics.pose();

            for (int i = 0; i < bodies.size(); i++) {
               DamageNumberBody body = bodies.get(i);
               if (body.active) {
                  body.getInterpolatedPosition(partialTick, this.interpolatedPos);
                  // ── 改版：3D 世界锚定 ──
                  // 锚点每帧重投影（随相机透视），再把屏幕空间的物理位移当作相对偏移叠加上去，
                  // 这样既保留弹性/重力手感，又让数字真正“贴”在被击中的位置上。
                  boolean anchored = body.worldAnchored && (Boolean)DamagePhysicsConfig.ENABLE_WORLD_SPACE.get();
                  float drawX = this.interpolatedPos.x;
                  float drawY = this.interpolatedPos.y;
                  float depthScale = 1.0F;
                  if (anchored) {
                     Vec2 anchor = this.projectToScreen(new Vec3(body.worldX, body.worldY, body.worldZ), this.projectedAnchor);
                     if (anchor == null) {
                        continue;
                     }
                     drawX = anchor.x + (this.interpolatedPos.x - body.spawnScreenX);
                     drawY = anchor.y + (this.interpolatedPos.y - body.spawnScreenY);
                     depthScale = this.worldSpaceScale(this.lastDepth);
                  }
                  if (!(drawX < -80.0F)
                     && !(drawX > (float)screenWidth + 80.0F)
                     && !(drawY < -80.0F)
                     && !(drawY > (float)screenHeight + 80.0F)) {
                     float alpha = body.getAlpha();
                     if (!(alpha <= 0.01F)) {
                        int drawColor = DamageNumberStyle.applyAlpha(body.color, alpha);
                        // ── 改版：高于阈值走动态彩虹；附魔类命中加紫色荧光线 ──
                        drawColor = this.resolveRainbow(body, drawColor, alpha);
                        float scale = body.baseScale * body.getPopScale() * depthScale;
                        float rotAngle = body.getInterpolatedAngle(partialTick);
                        poseStack.pushPose();
                        poseStack.translate(drawX, drawY, 0.0F);
                        poseStack.scale(scale * body.deformationX, scale * body.deformationY, 1.0F);
                        if (Math.abs(rotAngle) > 0.05F) {
                           poseStack.mulPose(Axis.ZP.rotationDegrees(rotAngle));
                        }

                        int textWidth = font.width(body.text);
                        int textHeight = 9;
                        this.drawWithOutline(guiGraphics, font, body, textWidth, textHeight, drawColor, alpha);
                        poseStack.popPose();
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * 世界空间缩放：{@code clamp(referenceDepth / depth, min, max)}。
    * 参考深度处为 1.0，越近越大（封顶 max）、越远越小（封底 min）。
    */
   private float worldSpaceScale(double depth) {
      double reference = (Double)DamagePhysicsConfig.WORLD_SPACE_REFERENCE_DEPTH.get();
      double lowerClamp = (Double)DamagePhysicsConfig.WORLD_SPACE_SCALE_MIN.get();
      double upperClamp = (Double)DamagePhysicsConfig.WORLD_SPACE_SCALE_MAX.get();
      if (depth <= 1.0E-3) {
         return (float)upperClamp;
      }
      double factor = reference / depth;
      return (float)Math.max(lowerClamp, Math.min(upperClamp, factor));
   }

   /**
    * 高于阈值时把数字颜色替换为动态彩虹色（HSV 色相随时间循环）。
    * 阈值 <= 0 视为关闭（配置项 {@code damageScale.rainbowThreshold}）。
    */
   private int resolveRainbow(DamageNumberBody body, int fallbackColor, float alpha) {
      if (!body.rainbow) {
         return fallbackColor;
      }
      double speed = (Double)DamagePhysicsConfig.RAINBOW_SPEED.get();
      float turns = (float)((double)(Util.getMillis() % 100000L) / 1000.0 * speed);
      float hue = turns - (float)Math.floor(turns);
      int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);
      return DamageNumberStyle.applyAlpha(rgb, alpha);
   }

   /**
    * 附魔类命中（其颜色等于 {@code colorEnchanted}）时，先在四周画一圈描边再画正文，
    * 形成「紫色荧光线」效果；其它数字保持原样单次绘制。
    */
   private void drawWithOutline(GuiGraphics guiGraphics, Font font, DamageNumberBody body, int textWidth, int textHeight, int drawColor, float alpha) {
      boolean outline = (Boolean)DamagePhysicsConfig.ENABLE_OUTLINE.get()
         && (Boolean)DamagePhysicsConfig.ENABLE_TYPE_OUTLINE.get()
         && body.typeColor != 0;
      int x = -textWidth / 2;
      int y = -textHeight / 2;
      if (outline) {
         double darken = (Double)DamagePhysicsConfig.OUTLINE_DARKEN.get();
         int outlineColorWithAlpha = DamageNumberStyle.applyAlpha(com.damagephysics.fork.ForkStyle.darken(body.typeColor, (float)darken), alpha);
         int thickness = (Integer)DamagePhysicsConfig.OUTLINE_WIDTH.get();
         boolean fourOnly = (Boolean)DamagePhysicsConfig.OUTLINE_FOUR_NEIGHBOUR_ONLY.get();
         // 四邻域：只上下左右，描边紧贴字形；八邻域会把放大后的字挤成一团（用户实测就是这个问题）
         int[][] steps = fourOnly
            ? new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}
            : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
         for (int step = 1; step <= thickness; step++) {
            for (int[] d : steps) {
               guiGraphics.drawString(font, body.text, x + d[0] * step, y + d[1] * step, outlineColorWithAlpha, false);
            }
         }
      }
      guiGraphics.drawString(font, body.text, x, y, drawColor, true);
   }

   /**
    * 解析 {@code #RRGGBB}（或 {@code #AARRGGBB}）颜色；解析失败返回 {@code fallback}，不抛异常。
    * 解析失败属于配置写错，用回退色比崩溃更合适；调用方无需处理异常。
    */
   private static int parseHexOrDefault(String hex, int fallback) {
      return com.damagephysics.fork.ForkStyle.parseHexOrDefault(hex, fallback);
   }
}
