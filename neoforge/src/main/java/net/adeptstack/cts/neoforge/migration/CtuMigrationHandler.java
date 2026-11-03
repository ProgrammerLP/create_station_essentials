package net.adeptstack.cts.neoforge.migration;

import net.adeptstack.cts.migration.CtuMigration;
import net.adeptstack.cts.migration.MigrationTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runtime converter for worlds that still contain CTU v3.x blocks while CTU is installed.
 * <p>
 * Chunk loads only look at the section palettes and queue chunks. The actual replacement runs in
 * the server tick with a configurable budget. Blocks are written straight into the chunk section:
 * the normal setBlock path would call CTU's onRemove, which tears down the other five parts of
 * a noise isolation wall.
 */
public final class CtuMigrationHandler {

    private static final Logger LOGGER = CtuMigration.LOGGER;
    private static final int SUMMARY_INTERVAL_TICKS = 20 * 60;
    private static final int SECTION_CELLS = 16 * 16 * 16;

    /** {@code null} while the converter is inactive. */
    private volatile MigrationTable table;

    /** Chunk loads can be reported off the server thread; they are moved into {@link #tasks} in the tick. */
    private final Queue<Request> incoming = new ConcurrentLinkedQueue<>();
    private final ArrayDeque<ChunkTask> tasks = new ArrayDeque<>();
    private final Map<TaskKey, ChunkTask> tasksByKey = new HashMap<>();

    private final Stats sinceSummary = new Stats();
    private final Stats total = new Stats();
    private long tickCounter;
    private boolean capabilityErrorLogged;

    // ---------------------------------------------------------------- lifecycle

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        reset();
        if (!MigrationConfig.ENABLED.get()) {
            LOGGER.info("CTU migration is disabled in the config");
            return;
        }
        MigrationTable resolved = CtuMigration.resolve();
        if (resolved.isEmpty()) {
            LOGGER.debug("No Create Train Utilities blocks registered, converter stays inactive");
            return;
        }
        table = resolved;
        LOGGER.info("Create Train Utilities is installed: converting {} old blocks and {} old items to Create Station Essentials",
                resolved.blockCount(), resolved.itemCount());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (table != null) {
            logSummary(total, "in total this session");
        }
        reset();
    }

    private void reset() {
        table = null;
        incoming.clear();
        tasks.clear();
        tasksByKey.clear();
        sinceSummary.clear();
        total.clear();
        tickCounter = 0;
        capabilityErrorLogged = false;
    }

    // ---------------------------------------------------------------- detection

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        MigrationTable t = table;
        if (t == null || event.isNewChunk()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        boolean blocks = containsOldBlocks(chunk, t);
        // getBlockEntitiesPos also covers block entities that are not created yet.
        boolean containers = t.hasItems() && !chunk.getBlockEntitiesPos().isEmpty();
        if (blocks || containers) {
            incoming.add(new Request(level.dimension(), chunk.getPos().toLong(), blocks, containers));
        }
    }

    /** Catches old blocks placed while both mods are loaded, so they don't wait for the next chunk load. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        MigrationTable t = table;
        if (t == null || !(event.getLevel() instanceof ServerLevel level) || !t.isOldState(event.getPlacedBlock())) {
            return;
        }
        // A noise isolation wall is two blocks wide and may reach into the neighbouring chunk.
        BlockPos pos = event.getPos();
        List<Long> chunks = new ArrayList<>(3);
        chunks.add(ChunkPos.asLong(pos));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            long neighbour = ChunkPos.asLong(pos.relative(direction));
            if (!chunks.contains(neighbour)) {
                chunks.add(neighbour);
            }
        }
        for (long chunkPos : chunks) {
            incoming.add(new Request(level.dimension(), chunkPos, true, false));
        }
    }

    private static boolean containsOldBlocks(LevelChunk chunk, MigrationTable t) {
        for (LevelChunkSection section : chunk.getSections()) {
            // maybeHas only checks the palette, not the 4096 blocks.
            if (!section.hasOnlyAir() && section.maybeHas(t::isOldState)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- players

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (table != null && event.getEntity() instanceof ServerPlayer player) {
            convertPlayer(player, table);
        }
    }

    private void convertPlayer(ServerPlayer player, MigrationTable t) {
        int changed = t.convertContainer(player.getInventory());
        changed += t.convertContainer(player.getEnderChestInventory());

        ItemStack carried = t.convert(player.containerMenu.getCarried());
        if (carried != null) {
            player.containerMenu.setCarried(carried);
            changed++;
        }

        if (changed > 0) {
            sinceSummary.playerStacks += changed;
            total.playerStacks += changed;
            LOGGER.debug("Converted {} item stacks of player {}", changed, player.getGameProfile().getName());
        }
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MigrationTable t = table;
        if (t == null) {
            return;
        }
        MinecraftServer server = event.getServer();

        Request request;
        while ((request = incoming.poll()) != null) {
            enqueue(request);
        }

        int budget = MigrationConfig.OPERATIONS_PER_TICK.get();
        while (budget > 0 && !tasks.isEmpty()) {
            ChunkTask task = tasks.peekFirst();
            budget = process(server, t, task, budget);
            if (task.isDone()) {
                tasks.pollFirst();
                tasksByKey.remove(task.key);
            }
        }

        tickCounter++;
        int interval = MigrationConfig.INVENTORY_INTERVAL_TICKS.get();
        if (interval > 0 && tickCounter % interval == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                convertPlayer(player, t);
            }
        }

        if (tickCounter % SUMMARY_INTERVAL_TICKS == 0 && !sinceSummary.isEmpty()) {
            logSummary(sinceSummary, "in the last 60 s");
            sinceSummary.clear();
        }
    }

    private void enqueue(Request request) {
        TaskKey key = new TaskKey(request.dimension, request.chunkPos);
        ChunkTask task = tasksByKey.get(key);
        if (task == null) {
            task = new ChunkTask(key);
            tasksByKey.put(key, task);
            tasks.addLast(task);
        }
        if (request.blocks && !task.convertBlocks) {
            task.convertBlocks = true;
            task.sectionIndex = 0;
            task.cellIndex = 0;
        }
        if (request.containers && !task.scanContainers) {
            task.scanContainers = true;
            task.containerPositions = null;
            task.containerIndex = 0;
        }
    }

    /** Works on one chunk and returns the budget that is left. */
    private int process(MinecraftServer server, MigrationTable t, ChunkTask task, int budget) {
        ServerLevel level = server.getLevel(task.key.dimension);
        ChunkPos chunkPos = new ChunkPos(task.key.chunkPos);
        LevelChunk chunk = level == null ? null : level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            // Unloaded in the meantime. It is queued again on its next load.
            task.convertBlocks = false;
            task.scanContainers = false;
            return budget;
        }

        if (task.convertBlocks) {
            budget = convertBlocks(level, chunk, t, task, budget);
            if (budget <= 0) {
                return 0;
            }
        }
        if (task.scanContainers) {
            budget = convertContainers(level, chunk, t, task, budget);
        }
        if (task.isDone() && task.blocksChanged > 0) {
            sinceSummary.chunks++;
            total.chunks++;
        }
        return budget;
    }

    // ---------------------------------------------------------------- blocks

    private int convertBlocks(ServerLevel level, LevelChunk chunk, MigrationTable t, ChunkTask task, int budget) {
        LevelChunkSection[] sections = chunk.getSections();
        while (task.sectionIndex < sections.length) {
            LevelChunkSection section = sections[task.sectionIndex];
            if (task.cellIndex == 0 && (section.hasOnlyAir() || !section.maybeHas(t::isOldState))) {
                task.sectionIndex++;
                continue;
            }

            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(task.sectionIndex));
            while (task.cellIndex < SECTION_CELLS) {
                int index = task.cellIndex++;
                int x = index & 15;
                int z = (index >> 4) & 15;
                int y = index >> 8;

                BlockState oldState = section.getBlockState(x, y, z);
                BlockState newState = t.convert(oldState);
                if (newState == null) {
                    continue;
                }
                replace(level, chunk, section, x, y, z, baseY, oldState, newState);
                task.blocksChanged++;
                if (--budget <= 0) {
                    return 0;
                }
            }
            task.sectionIndex++;
            task.cellIndex = 0;
        }
        task.convertBlocks = false;
        return budget;
    }

    private void replace(ServerLevel level, LevelChunk chunk, LevelChunkSection section,
                         int x, int y, int z, int baseY, BlockState oldState, BlockState newState) {
        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);

        // No onRemove/onPlace and no neighbour updates: both states have the same shape.
        section.setBlockState(x, y, z, newState);
        for (Map.Entry<Heightmap.Types, Heightmap> heightmap : chunk.getHeightmaps()) {
            heightmap.getValue().update(x, pos.getY(), z, newState);
        }
        if (LightEngine.hasDifferentLightProperties(chunk, pos, oldState, newState)) {
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }
        chunk.setUnsaved(true);
        // Sends the change to every player watching this chunk.
        level.getChunkSource().blockChanged(pos);

        sinceSummary.blocks++;
        total.blocks++;
        LOGGER.debug("Converted {} -> {} at {} in {}", oldState, newState, pos.toShortString(), level.dimension().location());
    }

    // ---------------------------------------------------------------- containers

    private int convertContainers(ServerLevel level, LevelChunk chunk, MigrationTable t, ChunkTask task, int budget) {
        if (task.containerPositions == null) {
            task.containerPositions = new ArrayList<>(chunk.getBlockEntitiesPos());
        }
        while (task.containerIndex < task.containerPositions.size()) {
            BlockPos pos = task.containerPositions.get(task.containerIndex++);
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            if (blockEntity != null) {
                int changed = convertBlockEntity(level, blockEntity, t);
                if (changed > 0) {
                    sinceSummary.containerStacks += changed;
                    total.containerStacks += changed;
                    LOGGER.debug("Converted {} item stacks in {} at {} in {}", changed,
                            BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()), pos.toShortString(), level.dimension().location());
                }
            }
            if (--budget <= 0) {
                break;
            }
        }
        if (task.containerIndex >= task.containerPositions.size()) {
            task.scanContainers = false;
            task.containerPositions = null;
        }
        return Math.max(budget, 0);
    }

    private int convertBlockEntity(ServerLevel level, BlockEntity blockEntity, MigrationTable t) {
        int changed;
        if (blockEntity instanceof Container container) {
            changed = t.convertContainer(container);
        } else {
            changed = convertItemHandler(level, blockEntity, t);
        }
        if (changed > 0) {
            blockEntity.setChanged();
            BlockState state = blockEntity.getBlockState();
            // Lets block entities that show their items (e.g. Create depots) update on the client.
            level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
        return changed;
    }

    /** Covers modded storage that only exposes an item handler, e.g. Create vaults. */
    private int convertItemHandler(ServerLevel level, BlockEntity blockEntity, MigrationTable t) {
        IItemHandler handler;
        try {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(),
                    blockEntity.getBlockState(), blockEntity, null);
        } catch (RuntimeException e) {
            // Another mod's capability provider failed. Skip this block entity instead of crashing the tick.
            if (!capabilityErrorLogged) {
                capabilityErrorLogged = true;
                LOGGER.warn("Could not read the inventory of {} at {}; skipping such block entities",
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()), blockEntity.getBlockPos().toShortString(), e);
            }
            return 0;
        }
        if (!(handler instanceof IItemHandlerModifiable modifiable)) {
            return 0;
        }
        int changed = 0;
        for (int slot = 0; slot < modifiable.getSlots(); slot++) {
            ItemStack converted = t.convert(modifiable.getStackInSlot(slot));
            if (converted != null) {
                modifiable.setStackInSlot(slot, converted);
                changed++;
            }
        }
        return changed;
    }

    // ---------------------------------------------------------------- logging

    private void logSummary(Stats stats, String period) {
        if (stats.isEmpty()) {
            return;
        }
        LOGGER.info("CTU migration {}: {} blocks in {} chunks, {} item stacks in containers, {} item stacks in player inventories ({} chunks queued)",
                period, stats.blocks, stats.chunks, stats.containerStacks, stats.playerStacks, tasks.size());
    }

    // ---------------------------------------------------------------- data

    private record Request(ResourceKey<Level> dimension, long chunkPos, boolean blocks, boolean containers) {
    }

    private record TaskKey(ResourceKey<Level> dimension, long chunkPos) {
    }

    private static final class ChunkTask {
        final TaskKey key;
        boolean convertBlocks;
        int sectionIndex;
        int cellIndex;
        int blocksChanged;
        boolean scanContainers;
        List<BlockPos> containerPositions;
        int containerIndex;

        ChunkTask(TaskKey key) {
            this.key = key;
        }

        boolean isDone() {
            return !convertBlocks && !scanContainers;
        }
    }

    private static final class Stats {
        long blocks;
        long chunks;
        long containerStacks;
        long playerStacks;

        boolean isEmpty() {
            return blocks == 0 && containerStacks == 0 && playerStacks == 0;
        }

        void clear() {
            blocks = 0;
            chunks = 0;
            containerStacks = 0;
            playerStacks = 0;
        }
    }
}
