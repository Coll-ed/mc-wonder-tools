package com.damagephysics.render;

import com.damagephysics.particle.ImpactSparkManager;
import com.damagephysics.physics.DamageNumberBody;
import com.damagephysics.physics.DamagePhysicsWorld;
import com.damagephysics.physics.Vec2;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
                  if (!(this.interpolatedPos.x < -80.0F)
                     && !(this.interpolatedPos.x > (float)screenWidth + 80.0F)
                     && !(this.interpolatedPos.y < -80.0F)
                     && !(this.interpolatedPos.y > (float)screenHeight + 80.0F)) {
                     float alpha = body.getAlpha();
                     if (!(alpha <= 0.01F)) {
                        int drawColor = DamageNumberStyle.applyAlpha(body.color, alpha);
                        float scale = body.baseScale * body.getPopScale();
                        float rotAngle = body.getInterpolatedAngle(partialTick);
                        poseStack.pushPose();
                        poseStack.translate(this.interpolatedPos.x, this.interpolatedPos.y, 0.0F);
                        poseStack.scale(scale * body.deformationX, scale * body.deformationY, 1.0F);
                        if (Math.abs(rotAngle) > 0.05F) {
                           poseStack.mulPose(Axis.ZP.rotationDegrees(rotAngle));
                        }

                        int textWidth = font.width(body.text);
                        int textHeight = 9;
                        guiGraphics.drawString(font, body.text, -textWidth / 2, -textHeight / 2, drawColor, true);
                        poseStack.popPose();
                     }
                  }
               }
            }
         }
      }
   }
}
