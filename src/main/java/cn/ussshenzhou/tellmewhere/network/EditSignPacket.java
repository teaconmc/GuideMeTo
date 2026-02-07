package cn.ussshenzhou.tellmewhere.network;

import cn.ussshenzhou.t88.network.annotation.Decoder;
import cn.ussshenzhou.t88.network.annotation.Encoder;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import cn.ussshenzhou.tellmewhere.TellMeWhere;
import cn.ussshenzhou.tellmewhere.blockentity.SignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = TellMeWhere.MODID)
public class EditSignPacket {
    public BlockPos pos;
    public Map<String, String> languageAndText;
    public int backgroundArgb;
    public int foregroundArgb;

    public EditSignPacket(BlockPos pos, Map<String, String> languageAndText, int backgroundArgb, int foregroundArgb) {
        this.pos = pos;
        this.languageAndText = languageAndText;
        this.backgroundArgb = backgroundArgb;
        this.foregroundArgb = foregroundArgb;
    }

    @Decoder
    public EditSignPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.languageAndText = buf.readMap(FriendlyByteBuf::readUtf, b -> b.readUtf());
        this.backgroundArgb = buf.readInt();
        this.foregroundArgb = buf.readInt();
    }

    @Encoder
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeMap(languageAndText, FriendlyByteBuf::writeUtf, (b, s) -> b.writeUtf(s));
        buf.writeInt(backgroundArgb);
        buf.writeInt(foregroundArgb);
    }

    @ServerHandler
    public void handler(IPayloadContext context) {
        var level = context.player().level();
        if (context.player().isCreative() && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity) {
            signBlockEntity.getData().setBackgroundArgb(backgroundArgb);
            signBlockEntity.getData().setForegroundArgb(foregroundArgb);
            signBlockEntity.setRawTexts(languageAndText);
        }
    }

}
