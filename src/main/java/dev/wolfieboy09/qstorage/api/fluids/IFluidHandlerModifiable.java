package dev.wolfieboy09.qstorage.api.fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface IFluidHandlerModifiable extends IFluidHandler {
    void setStackInTank(int index, FluidStack fluidStack);
}
