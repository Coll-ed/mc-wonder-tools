package com.damagephysics.event;

import com.damagephysics.particle.ImpactSparkManager;
import com.damagephysics.physics.DamagePhysicsWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
   private static final ClientTickHandler INSTANCE = new ClientTickHandler();

   public static ClientTickHandler getInstance() {
      return INSTANCE;
   }

   private ClientTickHandler() {
   }

   @SubscribeEvent
   public void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.isPaused()) {
         ClientLevel level = mc.level;
         if (level != null) {
            DamageEventListener.getInstance().scanEntityHealth(level);
            DamagePhysicsWorld.getInstance().tick();
            ImpactSparkManager.getInstance().tick();
         }
      }
   }

   @SubscribeEvent
   public void onPlayerLogOut(LoggingOut event) {
      DamagePhysicsWorld.getInstance().clear();
      ImpactSparkManager.getInstance().clear();
      DamageEventListener.getInstance().clear();
   }
}
