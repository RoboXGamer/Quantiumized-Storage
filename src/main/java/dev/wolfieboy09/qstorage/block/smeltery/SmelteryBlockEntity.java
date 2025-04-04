package dev.wolfieboy09.qstorage.block.smeltery;

import dev.wolfieboy09.qstorage.QuantiumizedStorage;
import dev.wolfieboy09.qstorage.api.BiHolder;
import dev.wolfieboy09.qstorage.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qstorage.api.fluids.ExtendedFluidTank;
import dev.wolfieboy09.qstorage.api.recipes.CombinedRecipeInput;
import dev.wolfieboy09.qstorage.block.GlobalBlockEntity;
import dev.wolfieboy09.qstorage.registries.QSBlockEntities;
import dev.wolfieboy09.qstorage.registries.QSRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

@NothingNullByDefault
public class SmelteryBlockEntity extends GlobalBlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 10000;
    public static final int INPUT_TANKS_COUNT = 3;
    private int progress = 0;
    private int crafting_ticks = 0;
    private boolean isValidRecipe = false;
    @UnknownNullability
    private SmelteryRecipe recipe = null;

    private final Component TITLE = Component.translatable("block.qstorage.smeltery");
    // Expand container data to include both fluid IDs and amounts
    // For each tank: [fluidId, amount]
    // So for 3 tanks we need 6 integers
    private final ContainerData containerData = new SimpleContainerData(INPUT_TANKS_COUNT * 2);
    private final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < 2) {
                boolean isValidRecipe = checkRecipe();
                if (!isValidRecipe) resetProgress();
                setIsValidRecipe(isValidRecipe);
            }
            SmelteryBlockEntity.this.onContentsChanged();
        }
    };
    private final List<ExtendedFluidTank> inputTanks = new ArrayList<>(INPUT_TANKS_COUNT);
    private final ExtendedFluidTank outputFluidTank = new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged);
    private final ExtendedFluidTank wasteOutputFluidTank = new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged);
    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 5;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().getFluid();
                case 1 -> inputTanks.get(1).getFluid();
                case 2 -> inputTanks.get(2).getFluid();
                case 3 -> outputFluidTank.getFluid();
                case 4 -> wasteOutputFluidTank.getFluid();
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for getting a fluid tank");
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().getCapacity();
                case 1 -> inputTanks.get(1).getCapacity();
                case 2 -> inputTanks.get(2).getCapacity();
                case 3 -> outputFluidTank.getCapacity();
                case 4 -> wasteOutputFluidTank.getCapacity();
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for getting a fluid tank capacity");
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().isFluidValid(stack);
                case 1 -> inputTanks.get(1).isFluidValid(stack);
                case 2 -> inputTanks.get(2).isFluidValid(stack);
                case 3 -> outputFluidTank.isFluidValid(stack);
                case 4 -> wasteOutputFluidTank.isFluidValid(stack);
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for fluid validation");
            };
        }

        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction) {
            onContentsChanged();
            for (ExtendedFluidTank inputTank : inputTanks) {
                if (inputTank.getFluid().getFluid() == fluidStack.getFluid()) {
                    return inputTank.fill(fluidStack, fluidAction);
                } else if (inputTank.getFluid().isEmpty()) {
                    return inputTank.fill(fluidStack, fluidAction);
                }
            }
        //    Cant fill into the output tanks
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
            onContentsChanged();
            if (outputFluidTank.getFluid().getFluid() == fluidStack.getFluid()) {
                return outputFluidTank.drain(fluidStack, fluidAction);
            }
            if (wasteOutputFluidTank.getFluid().getFluid() == fluidStack.getFluid()) {
                return wasteOutputFluidTank.drain(fluidStack, fluidAction);
            }
        //    Cant extract out from the input tanks
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction fluidAction) {
            onContentsChanged();
            if (outputFluidTank.getFluidAmount() >= maxDrain) {
                return outputFluidTank.drain(maxDrain, fluidAction);
            }
            if (wasteOutputFluidTank.getFluidAmount() >= maxDrain) {
                return wasteOutputFluidTank.drain(maxDrain, fluidAction);
            }
        //    Cant extract out from the input tanks
            return FluidStack.EMPTY;
        }
    };

    public SmelteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(QSBlockEntities.SMELTERY.get(), pos, blockState);
        initInputFluidTanks();
    }

    public void onContentsChanged() {
        setChanged();
        updateContainerData();
    }

    public static class SmelterySlot {
        public static final int MAIN_ITEM_SLOT_1 = 0;
        public static final int MAIN_ITEM_SLOT_2 = 1;
        public static final int MAIN_ITEM_SLOT_3 = 2;
        public static final int RESULT_SLOT = 3;
        public static final int WASTE_RESULT_SLOT = 4;
        public static final int FLUID_SLOT_1 = 0;
        public static final int FLUID_SLOT_2 = 1;
        public static final int FLUID_SLOT_3 = 2;
        public static final int RESULT_FLUID_SLOT = 3;
        public static final int WASTE_RESULT_FLUID_SLOT = 4;
    }

    private void updateContainerData() {
        // Update container data with fluid IDs and amounts
        for (int i = 0; i < INPUT_TANKS_COUNT; i++) {
            FluidStack fluid = this.inputTanks.get(i).getFluid();
            // Store fluid registry ID as an integer
            int fluidId = BuiltInRegistries.FLUID.getId(fluid.getFluid());
            // Store amount
            int amount = fluid.getAmount();
            
            // Update container data (2 slots per tank)
            this.containerData.set(i * 2, fluidId);
            this.containerData.set(i * 2 + 1, amount);
        }
    }
    
    public void initInputFluidTanks() {
        for (int i = 0; i < INPUT_TANKS_COUNT; i++) {
            this.inputTanks.add(new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged));
        }
    }

    public @Nullable IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    public List<ExtendedFluidTank> getInputTanks() {
        return this.inputTanks;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public Component getDisplayName() {
        return this.TITLE;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        updateContainerData();
        return new SmelteryMenu(id, this.getBlockPos(), playerInv, player, this.containerData);
    }

    private CompoundTag saveFluidTank(FluidTank fluidTank, HolderLookup.Provider registries) {
        return fluidTank.writeToNBT(registries, new CompoundTag());
    }

    private void setIsValidRecipe(boolean val) {
        this.isValidRecipe = val;
    }

    private void loadFluidTank(FluidTank fluidTank, CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) return;
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveRecipeToNBT(tag);
        ListTag listTag = new ListTag();
        for (ExtendedFluidTank inputTank : this.inputTanks) {
            listTag.add(saveFluidTank(inputTank, registries));
        }

        tag.put("InputTanks", listTag);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        tag.put("OutputTank", saveFluidTank(this.outputFluidTank, registries));
        tag.put("WasteTank", saveFluidTank(this.wasteOutputFluidTank, registries));

        tag.putInt("progress", this.progress);
        tag.putInt("crafting_ticks", this.crafting_ticks);
        tag.putBoolean("isValidRecipe", this.isValidRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag listTag = tag.getList("InputTanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            loadFluidTank(this.inputTanks.get(i), listTag.getCompound(i), registries);
        }

        loadFluidTank(this.outputFluidTank, tag.getCompound("OutputTank"), registries);
        loadFluidTank(this.wasteOutputFluidTank, tag.getCompound("WasteTank"), registries);
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));

        this.progress = tag.getInt("progress");
        this.crafting_ticks = tag.getInt("crafting_ticks");
        this.isValidRecipe = tag.getBoolean("isValidRecipe");
        if (tag.contains("recipe")) {
            loadRecipeFromNBT(tag.getCompound("recipe"));
        }
    }

    public SimpleContainer getInputContainer() {
        SimpleContainer container = new SimpleContainer(4);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, this.inventory.getStackInSlot(i));
        }
        return container;
    }

    private final List<FluidTank> recipeTanks = new ArrayList<>();
    private final IFluidHandler recipeTanksHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 5;
        }

        @Override
        public FluidStack getFluidInTank(int i) {
            return recipeTanks.get(i).getFluid();
        }

        @Override
        public int getTankCapacity(int i) {
            return recipeTanks.get(i).getTankCapacity(i);
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return recipeTanks.get(i).isFluidValid(fluidStack);
        }

        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction) {
            int filled = 0;
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(fluidStack)) {
                    filled = recipeTank.fill(fluidStack, fluidAction);
                    return filled;
                }
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(fluidStack)) {
                    return recipeTank.drain(fluidStack, fluidAction);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int i, FluidAction fluidAction) {
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(recipeTank.getFluidInTank(i))) {
                    return recipeTank.drain(i, fluidAction);
                }
            }
            return FluidStack.EMPTY;
        }
    };


    private boolean checkRecipe() {
        if (this.level == null) return false;
        ItemStackHandler itemInputHandler = new ItemStackHandler(4);
        this.recipeTanks.clear();
        for (int i = 0; i < 4; i++) {
            this.recipeTanks.add(new FluidTank(TANK_CAPACITY));
        }
        for (int i = 0; i < 4; i++) {
            itemInputHandler.setStackInSlot(i, this.inventory.getStackInSlot(i));
            if (getFluidHandler() != null) recipeTanks.get(i).setFluid(getFluidHandler().getFluidInTank(i));
        }
        CombinedRecipeInput input = new CombinedRecipeInput(recipeTanksHandler, itemInputHandler);
        RecipeManager recipes = this.level.getRecipeManager();
        RecipeHolder<SmelteryRecipe> recipeFound = recipes.getRecipeFor(
                QSRecipes.SMELTERY_RECIPE_TYPE.get(),
                input,
                this.level
        ).orElse(null);
        if (recipeFound == null) return false;
        SmelteryRecipe recipeFetched = recipeFound.value();
        boolean matches = recipeFetched.matches(input, this.level);
        if (!matches) return false;
        // LEFT                                  |  RIGHT
        // Main Results (at least one required)  |  Waste (not required, can be null for both)
        BiHolder<BiHolder<ItemStack, FluidStack>, BiHolder<ItemStack, FluidStack>> results = recipeFetched.assembleRecipe(input, this.level.registryAccess());
        if (results.left().left() != null || results.left().right() != null) return false;
        this.recipe = recipeFound.value();
        return true;
    }

    public void tick() {
        if (!this.isValidRecipe || this.level == null || (getInputTanks().isEmpty() || getInputContainer().isEmpty())) return;

        ItemStack outputSlotStack = this.inventory.getStackInSlot(SmelterySlot.RESULT_SLOT);
        ItemStack resultItem = this.recipe.getResultItem(this.level.registryAccess());
        FluidStack resultFluidSlot = this.recipe.getFluidStackFromList(this.recipe.result());
        FluidTank outputFluidSlot = this.outputFluidTank;

        boolean outputItemHasSpace = outputSlotStack.isEmpty() ||
                (outputSlotStack.getItem() == resultItem.getItem() &&
                        outputSlotStack.getCount() + resultItem.getCount() <= outputSlotStack.getMaxStackSize());

        boolean outputFluidHasSpace = outputFluidTank.isEmpty() ||
                (outputFluidSlot.getFluidInTank(SmelterySlot.RESULT_FLUID_SLOT).getFluid() == resultFluidSlot.getFluid() && outputFluidSlot.getFluidInTank(SmelterySlot.RESULT_FLUID_SLOT).getAmount() + resultFluidSlot.getAmount() <= outputFluidSlot.getCapacity());


        // No room? Don't continue
        if (!outputItemHasSpace && !outputFluidHasSpace) return;

        int timeRequired = this.recipe.timeInTicks();

        if (this.crafting_ticks < timeRequired) {
            this.crafting_ticks++;
            this.progress = getProgress();
            setChanged();
        }

        if (this.progress >= 100) {
            consumeInputItems(recipe);
            if (outputSlotStack.isEmpty()) {
                this.inventory.setStackInSlot(SmelterySlot.RESULT_SLOT, resultItem.copy());
            } else {
                outputSlotStack.grow(resultItem.getCount());
            }
        }

        resetProgress();
    }

    private int getProgress() {
        return this.recipe == null ? 0 : (int) (this.crafting_ticks / (float) this.recipe.timeInTicks() * 100);
    }

    private void consumeInputItems(SmelteryRecipe recipe) {
        // TODO Have it shrink by recipe amount
        this.inventory.getStackInSlot(SmelterySlot.MAIN_ITEM_SLOT_1).shrink(1);
        this.inventory.getStackInSlot(SmelterySlot.MAIN_ITEM_SLOT_2).shrink(1);
        this.inventory.getStackInSlot(SmelterySlot.MAIN_ITEM_SLOT_2).shrink(1);
        // TODO Drain the fluid tanks by the recipe amount as well
    }

    protected void resetProgress() {
        this.crafting_ticks = 0;
        this.progress = 0;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateContainerData();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag toMerge = new CompoundTag();
        toMerge.putInt("crafting_ticks", this.crafting_ticks);
        return super.saveWithoutMetadata(registries).merge(toMerge);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.crafting_ticks = tag.getInt("crafting_ticks");
    }

    private void saveRecipeToNBT(CompoundTag nbt) {
        try {
            if (this.recipe instanceof SmelteryRecipe smelteryRecipe) {
                nbt.put("recipe", SmelteryRecipe.CODEC.encodeStart(NbtOps.INSTANCE, smelteryRecipe).getOrThrow());
            }
        } catch (Exception e) {
            QuantiumizedStorage.LOGGER.error("Error saving recipe to NBT: {}", e.getMessage());
        }
    }

    private void loadRecipeFromNBT(CompoundTag recipeTag) {
        if (Recipe.CODEC.parse(NbtOps.INSTANCE, recipeTag).getOrThrow() instanceof SmelteryRecipe smelteryRecipe) {
            this.recipe = smelteryRecipe;
        }
    }
}
