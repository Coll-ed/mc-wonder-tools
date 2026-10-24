package com.liuyulianse.flatxp.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Overrides the experience-per-level formula so every level costs
 * exactly 7 XP points (the vanilla cost for level 0→1).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /**
     * @reason Replace exponential XP scaling with fixed 7 XP per level.
     */
    @Overwrite
    public int getXpNeededForNextLevel() {
        return 7;
    }
}
