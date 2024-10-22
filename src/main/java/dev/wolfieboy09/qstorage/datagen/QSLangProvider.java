package dev.wolfieboy09.qstorage.datagen;

import dev.wolfieboy09.qstorage.QuantiumizedStorage;
import dev.wolfieboy09.qstorage.api.util.NamingUtil;
import dev.wolfieboy09.qstorage.registries.QSCreativeTab;
import dev.wolfieboy09.qstorage.registries.QSItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.HashMap;

public class QSLangProvider extends LanguageProvider {
    public QSLangProvider(PackOutput output) {
        super(output, QuantiumizedStorage.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        QSItems.ITEMS.getEntries().forEach(
                item -> addItem(item, NamingUtil.toHumanReadable(item.getRegisteredName().split(":")[1]))
        );
        QSCreativeTab.REGISTER.getEntries().forEach(
                tab -> add(tab.get().getDisplayName().getString(), QSCreativeTab.nameForLangGen.get(tab.get().getDisplayName().getString()))
        );
    }
}