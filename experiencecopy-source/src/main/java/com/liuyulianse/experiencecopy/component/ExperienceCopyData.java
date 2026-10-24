package com.liuyulianse.experiencecopy.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data stored on items with the Experience Copy component.
 *
 * @param originalMaxDamage        the original max durability of the source item
 * @param stage                    0=initial copy (1 durability), 1-5=upgrade stages
 * @param originalHadCustomMaxDamage whether the source item had a custom MAX_DAMAGE component
 */
public record ExperienceCopyData(
        int originalMaxDamage,
        int stage,
        boolean originalHadCustomMaxDamage
) {
    public static final Codec<ExperienceCopyData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("originalMaxDamage").forGetter(ExperienceCopyData::originalMaxDamage),
                    Codec.INT.fieldOf("stage").forGetter(ExperienceCopyData::stage),
                    Codec.BOOL.fieldOf("originalHadCustomMaxDamage").forGetter(ExperienceCopyData::originalHadCustomMaxDamage)
            ).apply(instance, ExperienceCopyData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceCopyData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExperienceCopyData::originalMaxDamage,
            ByteBufCodecs.VAR_INT, ExperienceCopyData::stage,
            ByteBufCodecs.BOOL, ExperienceCopyData::originalHadCustomMaxDamage,
            ExperienceCopyData::new
    );
}
