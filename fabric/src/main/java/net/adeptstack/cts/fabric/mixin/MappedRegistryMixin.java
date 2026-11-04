package net.adeptstack.cts.fabric.mixin;

import net.adeptstack.cts.migration.CtuMigration;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/**
 * Registry aliases old CTU id → id of this mod, which Fabric does not offer itself.
 * <p>
 * Chunk palettes, item stacks and recipes all resolve ids through {@code get(ResourceLocation)}.
 * The alias only applies when the old id is not registered, so while CTU v3.x is installed
 * nothing changes and the runtime converter does the work.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    @Shadow
    @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;

    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @ModifyVariable(method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation tracksta$resolveCtuAlias(ResourceLocation id) {
        // Cheap checks first: this method runs for every registry lookup in the game.
        if (id == null || !CtuMigration.OLD_NAMESPACE.equals(id.getNamespace()) || byLocation.containsKey(id)) {
            return id;
        }
        ResourceKey<?> registry = key();
        if (registry != Registries.BLOCK && registry != Registries.ITEM) {
            return id;
        }
        ResourceLocation target = CtuMigration.aliasTarget(id);
        return target != null ? target : id;
    }
}
