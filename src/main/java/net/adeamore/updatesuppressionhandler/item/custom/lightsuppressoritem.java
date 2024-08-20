package net.adeamore.updatesuppressionhandler.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class lightsuppressoritem extends Item {
    public lightsuppressoritem(Properties pProperties){
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        return super.useOn(pContext);
    }
}
