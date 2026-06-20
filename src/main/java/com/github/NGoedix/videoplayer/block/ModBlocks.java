package com.github.NGoedix.videoplayer.block;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.block.custom.RadioBlock;
import com.github.NGoedix.videoplayer.block.custom.TVBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;
import java.util.function.ToIntFunction;

public class ModBlocks {
    public static final Block TV_BLOCK = registerBlock("tv_block", TVBlock::new,
            BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(litBlockEmission(12)).strength(3.5F, 6.0F));

    public static final Block RADIO_BLOCK = registerBlock("radio_block", RadioBlock::new,
            BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(litBlockEmission(12)).strength(3.5F, 6.0F));

    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return (blockstate) -> blockstate.getValue(RedstoneTorchBlock.LIT) ? pLightValue : 0;
    }

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(Reference.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        T block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
        return block;
    }

    public static void registerModBlocks() {
        Reference.LOGGER.info("Registering block for " + Reference.MOD_ID);
    }
}
