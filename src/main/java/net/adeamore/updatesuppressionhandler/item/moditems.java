package net.adeamore.updatesuppressionhandler.item;

import net.adeamore.updatesuppressionhandler.item.custom.lightsuppressoritem;
import net.adeamore.updatesuppressionhandler.item.custom.updatesuppressoritem;
import net.adeamore.updatesuppressionhandler.updatesuppressionhandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class moditems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, updatesuppressionhandler.MOD_ID);

    public static final RegistryObject<Item> SUPPRESSOR_ITEM = ITEMS.register("suppressoritem",
            () -> new updatesuppressoritem(new Item.Properties()));

    public static final RegistryObject<Item> LIGHT_SUPPRESSION_ITEM = ITEMS.register("lightsuppressionitem",
            () -> new lightsuppressoritem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
