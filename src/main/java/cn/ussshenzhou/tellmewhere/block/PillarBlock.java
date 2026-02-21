package cn.ussshenzhou.tellmewhere.block;

import cn.ussshenzhou.tellmewhere.TellMeWhere;
import cn.ussshenzhou.tellmewhere.blockentity.SignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Vector3f;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * @author USS_Shenzhou
 */
public class PillarBlock extends BaseSignBlock{
    public PillarBlock(Properties properties, Vector3f screenStart16, int defaultScreenLength16, int screenHeight16, int screenThick16, int screenMargin16) {
        super(properties, screenStart16, defaultScreenLength16, screenHeight16, screenThick16, screenMargin16);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        SignBlockEntity signBlockEntity = (SignBlockEntity) level.getBlockEntity(pos);
        Item item = player.getItemInHand(hand).getItem();
        var itemName = BuiltInRegistries.ITEM.getKey(item);
        if (TellMeWhere.MODID.equals(itemName.getNamespace()) && (itemName.getPath().contains("sign_") || itemName.getPath().contains("pillar_"))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && item instanceof BlockItem blockItem) {
            //itemInHand can place a block
            BlockState blockState = blockItem.getBlock().defaultBlockState();
            //try set direction
            if (blockState.getOptionalValue(FACING).isPresent()) {
                blockState.setValue(FACING, state.getValue(FACING));
            }
            if (blockState.getShape(level, pos) == Shapes.block()
                    //block placed by itemInHand is a full block
                    && signBlockEntity.getDisguiseBlockState().getBlock() != blockItem.getBlock()) {
                //block placed by itemInHand is a new block
                signBlockEntity.setDisguise(blockState);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SignBlockEntity(pPos, pState, screenStart16, defaultScreenLength16, screenHeight16, screenThick16, screenMargin16) {
            @Override
            public void checkSlavesAt() {
                return;
            }
        };
    }
}
