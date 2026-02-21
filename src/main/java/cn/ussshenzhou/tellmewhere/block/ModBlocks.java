package cn.ussshenzhou.tellmewhere.block;

import cn.ussshenzhou.tellmewhere.TellMeWhere;
import cn.ussshenzhou.tellmewhere.item.SignBlockAndItemRegistryHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

/**
 * @author USS_Shenzhou
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, TellMeWhere.MODID);

    static {
        SignBlockAndItemRegistryHelper.registerBlock(BLOCKS);
    }

    public static final Supplier<Block> MISSING = BLOCKS.register("missing", () -> new Block(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TellMeWhere.MODID, "missing")))
    ));
}
