package cn.ussshenzhou.tellmewhere.util;

import cn.ussshenzhou.tellmewhere.TellMeWhere;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ModRenderTypes {
    public static final RenderPipeline BACKGROUND_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            //.withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(Identifier.fromNamespaceAndPath(TellMeWhere.MODID, "background"))
            .withCull(true)
            .build();

    public static final RenderType BACKGROUND = RenderType.create("guide_me_to_background", RenderSetup.builder(BACKGROUND_PIPELINE)
            //.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup());

    @SubscribeEvent
    public static void registerRenderPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BACKGROUND_PIPELINE);
    }
}
