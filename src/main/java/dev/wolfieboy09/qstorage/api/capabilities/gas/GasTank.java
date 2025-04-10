package dev.wolfieboy09.qstorage.api.capabilities.gas;

import dev.wolfieboy09.qstorage.api.registry.gas.GasStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class GasTank implements IGasHandler, IGasTank {
    protected Predicate<GasStack> validator;
    protected GasStack gas;
    protected int capacity;

    public GasTank(int capacity) {
        this(capacity, (e) -> true);
    }

    public GasTank(int capacity, Predicate<GasStack> validator) {
        this.gas = GasStack.EMPTY;
        this.capacity = capacity;
        this.validator = validator;
    }

    public GasTank setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public GasTank setValidator(Predicate<GasStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public boolean isGasValid(GasStack stack) {
        return this.validator.test(stack);
    }

    @Override
    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public GasStack getGas() {
        return this.gas;
    }

    @Override
    public int getGasAmount() {
        return this.gas.getAmount();
    }

    public GasTank readFromNBT(HolderLookup.Provider lookupProvider, @NotNull CompoundTag nbt) {
        this.gas = GasStack.parseOptional(lookupProvider, nbt.getCompound("Gas"));
        return this;
    }

    public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        if (!this.gas.isEmpty()) {
            nbt.put("Gas", this.gas.save(lookupProvider));
        }
        return nbt;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public GasStack getGasInTank(int index) {
        return this.getGas();
    }

    @Override
    public int getTankCapacity(int index) {
        return this.getCapacity();
    }

    @Override
    public boolean isGasValid(int index, GasStack gasStack) {
        return this.validator.test(gasStack);
    }

    protected void onContentsChanged() {}

    @Override
    public int fill(GasStack resource, boolean simulate) {
        if (!resource.isEmpty() && this.isGasValid(resource)) {
            if (simulate) {
                if (this.gas.isEmpty()) {
                    return Math.min(this.capacity, resource.getAmount());
                } else {
                    return !GasStack.isSameGasSameComponents(this.gas, resource) ? 0 : Math.min(this.capacity - this.gas.getAmount(), resource.getAmount());
                }
            } else if (this.gas.isEmpty()) {
                this.gas = resource.copyWithAmount(Math.min(this.capacity, resource.getAmount()));
                this.onContentsChanged();
                return this.gas.getAmount();
            } else if (!GasStack.isSameGasSameComponents(this.gas, resource)) {
                return 0;
            } else {
                int filled = this.capacity - this.gas.getAmount();
                if (resource.getAmount() < filled) {
                    this.gas.grow(resource.getAmount());
                    filled = resource.getAmount();
                } else {
                    this.gas.setAmount(this.capacity);
                }
                if (filled > 0) {
                    this.onContentsChanged();
                }
                return filled;
            }
        } else {
            return 0;
        }
    }

    @Override
    public GasStack drain(GasStack resource, boolean simulate) {
        return !resource.isEmpty() && GasStack.isSameGasSameComponents(resource, this.gas) ? this.drain(resource.getAmount(), simulate) : GasStack.EMPTY;
    }

    @Override
    public GasStack drain(int maxDrain, boolean simulate) {
        int drained = maxDrain;
        if (this.gas.getAmount() < maxDrain) {
            drained = this.gas.getAmount();
        }
        GasStack stack = this.gas.copyWithAmount(drained);
        if (!simulate && drained > 0) {
            this.gas.shrink(drained);
            this.onContentsChanged();;
        }
        return stack;
    }
}
