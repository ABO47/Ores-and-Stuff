package com.abo47.oresandstuff.integration.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import net.minecraft.world.level.block.Block;

/**
 * Jade ("What's That Look") integration.
 * <p>
 * This class is only ever instantiated by Jade itself (annotation scan on Forge,
 * "jade" entrypoint on Fabric), so without Jade installed none of this code runs.
 */
@WailaPlugin
public class OresAndStuffJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(OreNodeComponentProvider.INSTANCE, Block.class);
        registration.registerBlockIcon(OreNodeComponentProvider.INSTANCE, Block.class);
    }
}
