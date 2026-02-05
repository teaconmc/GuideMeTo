package cn.ussshenzhou.tellmewhere.blockentity;

import cn.ussshenzhou.tellmewhere.DirectionUtil;
import cn.ussshenzhou.tellmewhere.SignText;
import cn.ussshenzhou.tellmewhere.block.BaseSignBlock;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SignBlockEntity extends BlockEntity {
    public static final String RAW_TEXT = "tmw_rawtext";
    public static final String LIGHT = "tmw_light";
    public static final String DISGUISE = "tmw_disguise";
    public static final String SLAVE = "tmw_slave";
    public static final String LENGTH = "tmw_length";

    protected SignText signText = new SignText();
    protected short light = -1;
    protected BlockState disguiseBlockState = Blocks.AIR.defaultBlockState();
    protected boolean slave = false;
    public int screenLength16;

    public int defaultScreenLength16;
    public Vector3f screenStart16;
    public int screenHeight16;
    public int screenThick16;
    public int screenMargin16;
    //public final int textMargin = 0;

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
        screenLength16 = defaultScreenLength16;
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

    public void checkSlavesAt() {
        var slaves = this.findSlaves();
        this.slave = false;
        this.screenLength16 = defaultScreenLength16 + 16 * slaves.size();
        slaves.forEach(SignBlockEntity::setSlave);
        var last = slaves.isEmpty() ? this : slaves.get(slaves.size() - 1);
        if (last.getBlockState().getValue(FACING) != this.getBlockState().getValue(FACING)) {
            //master of the other direction
            last.setFree();
        }
        needBroadcastToClients();
    }

    public void setSlave() {
        this.slave = true;
        needBroadcastToClients();
    }

    public void setFree() {
        this.slave = false;
        needBroadcastToClients();
    }

    public boolean isSlave() {
        return slave;
    }

    public boolean isMaster() {
        return !slave;
    }

    public SignText getSignText() {
        return signText;
    }

    public void setRawTexts(Map<String, String> languageAndText) {
        this.setSignText(new SignText(languageAndText));
    }

    public void setSignText(SignText signText) {
        this.signText = signText;
        if (level != null) {
            if (!level.isClientSide()) {
                needBroadcastToClients();
            } else {
                this.signText.setUsableWidth(this.screenLength16, this.screenHeight16);
            }
        }
    }

    private void needBroadcastToClients() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_NONE);
        }
    }

    public short getLight() {
        return light;
    }

    public void setLight(short light) {
        this.light = light;
        needBroadcastToClients();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        if (this.defaultScreenLength16 == Integer.MAX_VALUE) {
            //load from disk
            BaseSignBlock block = (BaseSignBlock) this.getBlockState().getBlock();
            this.defaultScreenLength16 = block.defaultScreenLength16;
            this.screenStart16 = new Vector3f(block.screenStart16);
            this.screenHeight16 = block.screenHeight16;
            this.screenThick16 = block.screenThick16;
            this.screenMargin16 = block.screenMargin16;
        }
        super.loadAdditional(input);
        light = (short) input.getShortOr(LIGHT, (short) 240);
        disguiseBlockState = input.read(DISGUISE, BlockState.CODEC).orElseGet(Blocks.AIR::defaultBlockState);
        slave = input.getBooleanOr(SLAVE, false);
        screenLength16 = input.getIntOr(LENGTH, 0);
        if (level != null) {
            setDisguise(disguiseBlockState);
        }
        //keep it at last
        this.setSignText(input.read(RAW_TEXT, SignText.CODEC).orElse(new SignText()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putShort(LIGHT, light);
        output.store(DISGUISE, BlockState.CODEC, disguiseBlockState);
        output.putBoolean(SLAVE, slave);
        output.putInt(LENGTH, screenLength16);
        output.store(RAW_TEXT, SignText.CODEC, signText);
    }

    public void setDisguise(BlockState disguiseState) {
        disguiseBlockState = disguiseState;
        needBroadcastToClients();
        if (level.isClientSide()) {
            SignBlockEntityRenderer.calculateDisguiseModel(this);
        }
    }

    public BlockState getDisguiseBlockState() {
        return disguiseBlockState;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putShort(LIGHT, light);
        tag.put(DISGUISE, NbtUtils.writeBlockState(disguiseBlockState));
        tag.putBoolean(SLAVE, slave);
        tag.putInt(LENGTH, screenLength16);
        tag.store(RAW_TEXT, SignText.CODEC, signText);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
