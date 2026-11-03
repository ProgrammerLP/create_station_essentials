package net.adeptstack.cts.neoforge.migration;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings for the CTU → Create Station Essentials converter (config/tracksta-common.toml). */
public final class MigrationConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue OPERATIONS_PER_TICK;
    public static final ModConfigSpec.IntValue INVENTORY_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "Converts platform blocks and noise isolation walls from Create Train Utilities v3.x",
                "(mod id 'trainutilities') into the blocks of this mod.",
                "Only runs while CTU still registers the old blocks. Without CTU, registry aliases take over."
        ).push("ctu_migration");

        ENABLED = builder
                .comment("Convert old CTU blocks and items while CTU is installed.")
                .define("enabled", true);

        OPERATIONS_PER_TICK = builder
                .comment("Work done per server tick. One converted block or one checked block entity (e.g. a chest) counts as one operation.")
                .defineInRange("operationsPerTick", 256, 1, 100_000);

        INVENTORY_INTERVAL_TICKS = builder
                .comment("How often online players' inventories and ender chests are converted, in ticks (20 ticks = 1 second). 0 = only on login.")
                .defineInRange("inventoryIntervalTicks", 100, 0, 72_000);

        builder.pop();
        SPEC = builder.build();
    }

    private MigrationConfig() {
    }
}
