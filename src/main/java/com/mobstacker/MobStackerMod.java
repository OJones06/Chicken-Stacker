package com.mobstacker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobStackerMod implements ModInitializer {
    public static final String MOD_ID = "mobstacker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int MERGE_INTERVAL_TICKS = 100; // 5 seconds
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        // Periodic chicken merging
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // Extra drops when a stacked chicken dies
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ChickenEntity chicken) {
                int count = ((StackableChicken) chicken).mobstacker_getStackCount();
                if (count > 1 && chicken.getEntityWorld() instanceof ServerWorld serverWorld) {
                    dropExtraLoot(serverWorld, chicken, count - 1);
                }
            }
            return true; // Always allow death
        });

        LOGGER.info("Mob Stacker loaded");
    }

    private void onServerTick(MinecraftServer server) {
        if (++tickCounter < MERGE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        for (ServerWorld world : server.getWorlds()) {
            mergeChickensInWorld(world);
        }
    }

    private void mergeChickensInWorld(ServerWorld world) {
        // Group adult, alive chickens by block position
        Map<BlockPos, List<ChickenEntity>> chickensByPos = new HashMap<>();

        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof ChickenEntity chicken && chicken.isAlive() && !chicken.isBaby()) {
                BlockPos pos = chicken.getBlockPos();
                chickensByPos.computeIfAbsent(pos, k -> new ArrayList<>()).add(chicken);
            }
        }

        // Merge groups
        for (List<ChickenEntity> group : chickensByPos.values()) {
            if (group.size() <= 1) {
                continue;
            }

            // Pick the first chicken as the survivor
            ChickenEntity survivor = group.get(0);
            int totalCount = 0;

            for (ChickenEntity chicken : group) {
                totalCount += ((StackableChicken) chicken).mobstacker_getStackCount();
            }

            // Remove all others
            for (int i = 1; i < group.size(); i++) {
                group.get(i).discard();
            }

            // Update survivor's stack count
            ((StackableChicken) survivor).mobstacker_setStackCount(totalCount);
        }
    }

    private void dropExtraLoot(ServerWorld world, ChickenEntity chicken, int extraCount) {
        // Feathers: 0-2 per chicken
        int totalFeathers = 0;
        for (int i = 0; i < extraCount; i++) {
            totalFeathers += chicken.getRandom().nextInt(3);
        }
        dropInStacks(world, chicken, Items.FEATHER, totalFeathers);

        // Meat: 1 per chicken (cooked if on fire)
        Item meat = chicken.isOnFire() ? Items.COOKED_CHICKEN : Items.CHICKEN;
        dropInStacks(world, chicken, meat, extraCount);

        // XP: 1-3 per chicken
        int totalXp = 0;
        for (int i = 0; i < extraCount; i++) {
            totalXp += 1 + chicken.getRandom().nextInt(3);
        }
        ExperienceOrbEntity.spawn(world, chicken.getEntityPos(), totalXp);
    }

    private void dropInStacks(ServerWorld world, ChickenEntity chicken, Item item, int total) {
        while (total > 0) {
            int count = Math.min(total, 64);
            chicken.dropStack(world, new ItemStack(item, count));
            total -= count;
        }
    }
}
