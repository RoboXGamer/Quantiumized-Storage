package dev.wolfieboy09.qstorage.api.recipes;

import dev.wolfieboy09.qstorage.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qstorage.block.disk_assembler.DiskAssemblerRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NothingNullByDefault
public class DiskAssemblerBuilder implements RecipeBuilder {
    protected final ItemStack result;
    protected final Ingredient diskPort;
    protected final Ingredient diskCasing;
    protected final Ingredient screws;
    protected final List<Ingredient> extras;
    protected final int energyCost;
    protected final int timeInTicks;
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public DiskAssemblerBuilder(Ingredient diskPort, Ingredient diskCasing, Ingredient screws, List<Ingredient> extras, int energyCost, int timeInTicks, ItemStack result) {
        this.diskPort = diskPort;
        this.diskCasing = diskCasing;
        this.screws = screws;
        this.extras = extras;
        this.energyCost = energyCost;
        this.timeInTicks = timeInTicks;
        this.result = result;
    }


    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        Advancement.Builder advancement = output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement::addCriterion);
        DiskAssemblerRecipe recipe = new DiskAssemblerRecipe(this.diskPort, this.diskCasing, this.screws, this.extras, this.energyCost, this.timeInTicks, this.result);
        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    public static DiskAssemblerBuilder create(Ingredient diskPort, Ingredient diskCasing, Ingredient screws, List<Ingredient> extras, int energyCost, int timeInTicks, ItemStack result) {
        return new DiskAssemblerBuilder(diskPort, diskCasing, screws, extras, energyCost, timeInTicks, result);
    }
}