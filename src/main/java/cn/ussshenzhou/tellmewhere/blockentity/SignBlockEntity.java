package cn.ussshenzhou.tellmewhere.blockentity;

import cn.ussshenzhou.tellmewhere.DirectionUtil;
import cn.ussshenzhou.tellmewhere.SignText;
import cn.ussshenzhou.tellmewhere.block.BaseSignBlock;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SignBlockEntity extends BlockEntity {
    public SignBlockData data = new SignBlockData();

    public int defaultScreenLength16;
    public Vector3f screenStart16;
    public int screenHeight16;
    public int screenThick16;
    public int screenMargin16;
    //public final int textMargin = 0;

    /**
     * Highly dangerous but working.
     * @see <a href="https://holojaneway.uss-shenzhou.cn/holojaneway/0.2#onlyin%E4%BD%86%E6%98%AF%E4%B8%BA%E4%BB%80%E4%B9%88">HoloJaneway</a>
     */
    protected SingleVariant disguiseModel;

    public SignBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(pPos, pBlockState, new Vector3f(0, 0, 0), Integer.MAX_VALUE, 16, 16, 0);
    }

    public SignBlockEntity(BlockPos pPos, BlockState pBlockState, Vector3f screenStart16, int defaultScreenLength16, int screenHeight16, int screenThick16, int screenMargin16) {
        super(ModBlockEntityTypeRegistry.TEST_SIGN.get(), pPos, pBlockState);
        this.defaultScreenLength16 = defaultScreenLength16;
        this.screenStart16 = screenStart16;
        this.screenHeight16 = screenHeight16;
        this.screenThick16 = screenThick16;
        this.screenMargin16 = screenMargin16;
        data.setScreenLength16(defaultScreenLength16);
    }

    public void neighborUpdated() {
        this.findMasterAt(true).checkSlavesAt();
        this.findMasterAt(false).checkSlavesAt();
    }

    public SignBlockEntity findMasterAt(boolean left) {
        var t = this;
        Direction facing = t.getBlockState().getValue(FACING);
        if (left) {
            facing = facing.getOpposite();
        }
        while (true) {
            BlockEntity rightEntity = DirectionUtil.getBlockEntityAtRight(level, t.getBlockPos(), facing);
            if (rightEntity instanceof SignBlockEntity r
                    && r.getBlockState().getBlock() == this.getBlockState().getBlock()
                    && DirectionUtil.isParallel(r.getBlockState().getValue(FACING), facing)) {
                t = r;
            } else {
                break;
            }
        }
        return t;
    }

    public ArrayList<SignBlockEntity> findSlaves() {
        ArrayList<SignBlockEntity> slaves = new ArrayList<>();
        var t = this;
        Direction facing = t.getBlockState().getValue(FACING);
        while (true) {
            BlockEntity rightEntity = DirectionUtil.getBlockEntityAtLeft(level, t.getBlockPos(), facing);
            if (rightEntity instanceof SignBlockEntity l
                    && l.getBlockState().getBlock() == this.getBlockState().getBlock()
                    && DirectionUtil.isParallel(l.getBlockState().getValue(FACING), facing)) {
                t = l;
                slaves.add(l);
            } else {
                break;
            }
        }
        return slaves;
    }

    protected void needBroadcastToClients() {
        this.setChanged();
        if (this.getLevel() != null) {
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_NONE);
        }
    }

    public void checkSlavesAt() {
        var slaves = this.findSlaves();
        data.setSlave(false);
        data.setScreenLength16(defaultScreenLength16 + 16 * slaves.size());
        slaves.forEach(SignBlockEntity::setSlave);
        var last = slaves.isEmpty() ? this : slaves.get(slaves.size() - 1);
        if (last.getBlockState().getValue(FACING) != this.getBlockState().getValue(FACING)) {
            //master of the other direction
            last.setFree();
        }
        needBroadcastToClients();
    }

    public void setSlave() {
        data.setSlave(true);
        needBroadcastToClients();
    }

    public void setFree() {
        data.setSlave(false);
        needBroadcastToClients();
    }

    public void setRawTexts(Map<String, String> languageAndText) {
        this.updateSignText(new SignText(languageAndText));
        needBroadcastToClients();
    }

    public void updateSignText(SignText signText) {
        data.setSignText(signText);
        if (level != null) {
            if (!level.isClientSide()) {
                needBroadcastToClients();
            } else {
                data.getSignText().setUsableWidth(data.getScreenLength16(), this.screenHeight16);
            }
        }
    }

    public SignBlockData getData() {
        return data;
    }

    public boolean isMaster() {
        return !data.isSlave();
    }

    public SignText getSignText() {
        return data.getSignText();
    }


    public short getLight() {
        return data.getLight();
    }

    public BlockState getDisguiseBlockState() {
        return data.getDisguiseBlockState();
    }

    public int getScreenLength16() {
        return data.getScreenLength16();
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        //load from disk
        BaseSignBlock block = (BaseSignBlock) this.getBlockState().getBlock();
        this.defaultScreenLength16 = block.defaultScreenLength16;
        this.screenStart16 = new Vector3f(block.screenStart16);
        this.screenHeight16 = block.screenHeight16;
        this.screenThick16 = block.screenThick16;
        this.screenMargin16 = block.screenMargin16;

        super.loadAdditional(input);
        data = input.read("data", SignBlockData.CODEC).orElse(new SignBlockData());
        if (level != null) {
            setDisguise(data.getDisguiseBlockState());
        }
        updateSignText(data.getSignText());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("data", SignBlockData.CODEC, data);
    }

    public void setDisguise(BlockState disguiseState) {
        data.setDisguiseBlockState(disguiseState);
        needBroadcastToClients();
        if (level != null && level.isClientSide()) {
            SignBlockEntityRenderer.calculateDisguiseModel(this);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.store("data", SignBlockData.CODEC, data);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
