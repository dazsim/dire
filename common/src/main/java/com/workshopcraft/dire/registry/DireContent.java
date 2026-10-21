package com.workshopcraft.dire.registry;

import com.workshopcraft.dire.Dire;
import com.workshopcraft.dire.gas.GasBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Map;
import java.util.function.BiConsumer;

public final class DireContent {
    public static final TagKey<Block> IGNITES_GAS = TagKey.create(Registries.BLOCK, Dire.id("ignites_gas"));
    public static final TagKey<EntityType<?>> SMALL_GAS_SOURCES = TagKey.create(Registries.ENTITY_TYPE, Dire.id("releases_small_gas"));
    public static final TagKey<EntityType<?>> LARGE_GAS_SOURCES = TagKey.create(Registries.ENTITY_TYPE, Dire.id("releases_large_gas"));
    public static final TagKey<Item> HELD_IGNITES_GAS = TagKey.create(Registries.ITEM, Dire.id("held_ignites_gas"));
    public static final TagKey<Item> REPAIRS_GAS_MASK = TagKey.create(Registries.ITEM, Dire.id("repairs_gas_mask"));
    public static final ResourceKey<DamageType> GAS_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Dire.id("gas"));

    public static final ArmorMaterial GAS_MASK_MATERIAL = new ArmorMaterial(15, Map.of(ArmorType.HELMET, 1), 10,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_GAS_MASK,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Dire.id("gas_mask")));

    public static final ResourceKey<CreativeModeTab> COMBAT_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));
    public static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));

    public static GasBlock GAS;
    public static Item BOTTLE_OF_GAS;
    public static Item GAS_MASK;
    public static final SimpleParticleType GAS_WISP = new SimpleParticleType(false) {};

    private DireContent() {}

    public static void registerBlocks(BiConsumer<Identifier, Block> r) {
        GAS = new GasBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Dire.id("gas")))
                .mapColor(MapColor.NONE)
                .replaceable()
                .noCollision()
                .noOcclusion()
                .noLootTable()
                .strength(0.0F, 0.0F)
                .pushReaction(PushReaction.DESTROY));
        r.accept(Dire.id("gas"), GAS);
    }

    private static Item.Properties item(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Dire.id(name)));
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        BOTTLE_OF_GAS = new Item(item("bottle_of_gas").stacksTo(16));
        r.accept(Dire.id("bottle_of_gas"), BOTTLE_OF_GAS);
        GAS_MASK = new Item(item("gas_mask").humanoidArmor(GAS_MASK_MATERIAL, ArmorType.HELMET));
        r.accept(Dire.id("gas_mask"), GAS_MASK);
    }

    public static void registerParticles(BiConsumer<Identifier, ParticleType<?>> r) {
        r.accept(Dire.id("gas_wisp"), GAS_WISP);
    }

    public static <T> BiConsumer<Identifier, T> direct(Registry<? super T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }

    public static void registerAllDirect() {
        registerBlocks(direct(BuiltInRegistries.BLOCK));
        registerItems(direct(BuiltInRegistries.ITEM));
        registerParticles(direct(BuiltInRegistries.PARTICLE_TYPE));
    }
}
