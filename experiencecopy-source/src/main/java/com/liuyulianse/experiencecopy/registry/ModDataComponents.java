package com.liuyulianse.experiencecopy.registry;

import com.liuyulianse.experiencecopy.ExperienceCopyMod;
import com.liuyulianse.experiencecopy.component.ExperienceCopyData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers the EXPERIENCE_COPY custom DataComponentType.
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ExperienceCopyMod.MODID);

    public static final Supplier<DataComponentType<ExperienceCopyData>> EXPERIENCE_COPY =
            REGISTRY.register("experience_copy", () ->
                    DataComponentType.<ExperienceCopyData>builder()
                            .persistent(ExperienceCopyData.CODEC)
                            .networkSynchronized(ExperienceCopyData.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

    private ModDataComponents() {}
}
