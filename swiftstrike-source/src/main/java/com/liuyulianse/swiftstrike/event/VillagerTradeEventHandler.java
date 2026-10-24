package com.liuyulianse.swiftstrike.event;

import com.liuyulianse.swiftstrike.SwiftStrikeMod;
import com.liuyulianse.swiftstrike.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.Optional;

@EventBusSubscriber(modid = SwiftStrikeMod.MODID)
public final class VillagerTradeEventHandler {

    private VillagerTradeEventHandler() {}

    @SubscribeEvent
    public static void onVillagerTrades(final VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;

        event.getTrades().get(2).add((trader, random) -> {
            Holder<Enchantment> holder = getHolder(trader);
            return makeTrade(holder, 1, 12);
        });

        event.getTrades().get(3).add((trader, random) -> {
            Holder<Enchantment> holder = getHolder(trader);
            return makeTrade(holder, 2, 20);
        });

        event.getTrades().get(4).add((trader, random) -> {
            Holder<Enchantment> holder = getHolder(trader);
            return makeTrade(holder, 3, 30);
        });

        event.getTrades().get(5).add((trader, random) -> {
            Holder<Enchantment> holder = getHolder(trader);
            return makeTrade(holder, 5, 56);
        });
    }

    private static Holder<Enchantment> getHolder(net.minecraft.world.entity.Entity entity) {
        return entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.SWIFT_STRIKE);
    }

    private static MerchantOffer makeTrade(Holder<Enchantment> holder, int level, int emeraldCost) {
        int price = Math.min(emeraldCost, 64);
        ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level));
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, price),
                Optional.of(new ItemCost(Items.BOOK, 1)),
                book, 12, 5 * level, 0.05F);
    }
}
