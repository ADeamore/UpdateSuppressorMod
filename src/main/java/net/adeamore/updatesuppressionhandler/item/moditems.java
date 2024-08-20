package net.adeamore.updatesuppressionhandler.item;

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

    public static final RegistryObject<Item> TEMPITEM = ITEMS.register("tempitem",
            () -> new updatesuppressoritem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
