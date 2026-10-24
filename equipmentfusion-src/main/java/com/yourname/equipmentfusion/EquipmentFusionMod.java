package com.yourname.equipmentfusion;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.yourname.equipmentfusion.event.FusionAnvilHandler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(EquipmentFusionMod.MOD_ID)
public class EquipmentFusionMod {

    public static final String MOD_ID = "equipmentfusion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUSION_COUNT =
        DATA_COMPONENTS.register("fusion_count",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IMPERFECT =
        DATA_COMPONENTS.register("imperfect",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public EquipmentFusionMod(IEventBus modEventBus) {
        LOGGER.info("[EquipmentFusion] Loading mod...");
        LOGGER.info("[EquipmentFusion] Registering event handlers...");

        DATA_COMPONENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(new FusionAnvilHandler());

        LOGGER.info("[EquipmentFusion] Anvil fusion handler registered.");
        LOGGER.info("[EquipmentFusion] Attribute fusion system initialized.");
        LOGGER.info("[EquipmentFusion] Cost calculation system initialized.");
        LOGGER.info("[EquipmentFusion] Startup check passed.");
    }
}
