package net.adeamore.updatesuppressionhandler.item;

import net.adeamore.updatesuppressionhandler.updatesuppressionhandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class modcreativemodetabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, updatesuppressionhandler.MOD_ID);

    public static final RegistryObject<CreativeModeTab> UPDATE_SUPPRESSION_TAB = CREATIVE_MODE_TABS.register("tutorial_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(moditems.TEMPITEM.get()))
                    .title(Component.translatable("creativetab.updatesuppressor_tab"))
                    .displayItems(((pParameters, pOutput) -> {
                        pOutput.accept(moditems.TEMPITEM.get());
                    }))
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
