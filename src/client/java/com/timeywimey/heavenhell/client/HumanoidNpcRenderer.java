package com.timeywimey.heavenhell.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Draws our angels, imps and Lucifer with the player model and their own skin. */
public class HumanoidNpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;

	public HumanoidNpcRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		this.texture = texture;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}
}
