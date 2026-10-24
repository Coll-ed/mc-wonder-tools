package com.damagephysics;

import com.damagephysics.config.DamagePhysicsConfig;
import com.damagephysics.config.DamagePhysicsConfigScreen;
import com.damagephysics.event.ClientTickHandler;
import com.damagephysics.event.DamageEventListener;
import com.damagephysics.render.DamageNumberRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("damagephysics")
public class DamagePhysicsMod {
   public static final String MODID = "damagephysics";
   public static final String MOD_NAME = "DamagePhysics";
   private static final Logger LOGGER = LoggerFactory.getLogger("DamagePhysics");

   public DamagePhysicsMod(IEventBus modEventBus, ModContainer modContainer) {
      LOGGER.info("Initializing {} for NeoForge 1.21.1", "DamagePhysics");
      modContainer.registerConfig(Type.CLIENT, DamagePhysicsConfig.SPEC);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         modContainer.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(container, parent) -> new DamagePhysicsConfigScreen(parent));
         NeoForge.EVENT_BUS.register(DamageEventListener.getInstance());
         NeoForge.EVENT_BUS.register(ClientTickHandler.getInstance());
         NeoForge.EVENT_BUS.register(DamageNumberRenderer.getInstance());
         LOGGER.info("{} client-side physics and rendering registered successfully.", "DamagePhysics");
      }
   }
}
