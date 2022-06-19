package mca;

import com.google.common.collect.ImmutableMap;
import dev.architectury.registry.registries.RegistrySupplier;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.HashMap;
import java.util.Map;

public class TradeOffersMCA {
    public static Map<String, Pair<Integer, TradeOffers.Factory[]>> TRADES = new HashMap<>();

    public static void bootstrap() {
        TRADES.put("adventurer", new Pair<>(1, new TradeOffers.Factory[] {
                new SellItemFactory(Items.ENCHANTED_BOOK, 10, 16, 2),
                new SellItemFactory(Items.SLIME_BALL, 1, 10, 16, 1),
                new SellItemFactory(Items.LEATHER_HORSE_ARMOR, 3, 16, 10),
                new SellItemFactory(Items.SADDLE, 4, 1, 3, 5),
                new SellItemFactory(Items.IRON_HORSE_ARMOR, 5, 2, 20),
                new SellItemFactory(Items.DIAMOND, 4, 5, 20),
                new SellItemFactory(Items.GOLDEN_HORSE_ARMOR, 10, 1, 3, 30),
                new SellItemFactory(Items.GOLDEN_APPLE, 3, 1, 8, 30),
                new SellItemFactory(Items.DIAMOND_HORSE_ARMOR, 15, 1, 1, 30),
                new SellItemFactory(Items.ENCHANTED_GOLDEN_APPLE, 20, 1, 3, 50),
                new BuyForOneEmeraldFactory(Items.BREAD, 15, 10, 50)
        }));
    }


    static class BuyForOneEmeraldFactory implements TradeOffers.Factory {
        private final Item buy;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public BuyForOneEmeraldFactory(ItemConvertible item, int price, int maxUses, int experience) {
            this.buy = item.asItem();
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = new ItemStack(this.buy, this.price);
            return new TradeOffer(itemStack, new ItemStack(Items.EMERALD), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class SellItemFactory implements TradeOffers.Factory {
        private final ItemStack sell;
        private final int price;
        private final int count;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public SellItemFactory(Block block, int price, int count, int maxUses, int experience) {
            this(new ItemStack(block), price, count, maxUses, experience);
        }

        public SellItemFactory(Item item, int price, int count, int experience) {
            this(new ItemStack(item), price, count, 12, experience);
        }

        public SellItemFactory(Item item, int price, int count, int maxUses, int experience) {
            this(new ItemStack(item), price, count, maxUses, experience);
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience) {
            this(stack, price, count, maxUses, experience, 0.05f);
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience, float multiplier) {
            this.sell = stack;
            this.price = price;
            this.count = count;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new ItemStack(Items.EMERALD, this.price), new ItemStack(this.sell.getItem(), this.count), this.maxUses, this.experience, this.multiplier);
        }
    }
}
