/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.util.tile;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: SimpleSingleFluidTank
 * Created by HellFirePvP
 * Date: 30.06.2019 / 22:31
 */
public class SimpleSingleFluidTank implements IFluidTank, IFluidTankProperties, IFluidHandler {

    private int amount = 0;
    private Fluid fluid = null;
    private int maxCapacity;
    private Runnable onUpdate = null;

    private boolean allowInput = true, allowOutput = true;

    public List<Direction> accessibleSides = new ArrayList<>();

    private SimpleSingleFluidTank() {}

    public SimpleSingleFluidTank(int maxCapacity) {
        this(maxCapacity, Direction.values());
    }

    public SimpleSingleFluidTank(int capacity, Direction... accessibleFrom) {
        this.maxCapacity = Math.max(0, capacity);
        this.accessibleSides = Arrays.asList(accessibleFrom);
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    public void setAllowInput(boolean allowInput) {
        this.allowInput = allowInput;
    }

    public void setAllowOutput(boolean allowOutput) {
        this.allowOutput = allowOutput;
    }

    //returns min(toAdd, what can be added at most)
    public int getMaxAddable(int toAdd) {
        return Math.min(toAdd, maxCapacity - amount);
    }

    public int getMaxDrainable(int toDrain) {
        return Math.min(toDrain, amount);
    }

    //leftover amount that could not be added
    public int addAmount(int amount) {
        if (this.fluid == null) return amount;
        int addable = getMaxAddable(amount);
        this.amount += addable;
        if (Math.abs(addable) > 0 && this.onUpdate != null) {
            this.onUpdate.run();
        }
        return amount - addable;
    }

    //returns amount drained
    @Nullable
    public FluidStack drain(int amount) {
        if (this.fluid == null) return null;
        int drainable = getMaxDrainable(amount);
        this.amount -= drainable;
        Fluid drainedFluid = this.fluid;
        if (this.amount <= 0) {
            setFluid(null);
        }
        if (Math.abs(drainable) > 0 && this.onUpdate != null) {
            this.onUpdate.run();
        }
        return new FluidStack(drainedFluid, drainable);
    }

    @Nullable
    @Override
    public FluidStack getFluid() {
        if (fluid == null) return null;
        return new FluidStack(fluid, amount);
    }

    @Nullable
    public Fluid getTankFluid() {
        return fluid;
    }

    public void setFluid(@Nullable Fluid fluid) {
        boolean update = false;
        if (fluid != this.fluid) {
            this.amount = 0;
            update = true;
        }
        this.fluid = fluid;
        if (update && this.onUpdate != null) {
            this.onUpdate.run();
        }
    }

    @Override
    public int getFluidAmount() {
        return amount;
    }

    @Nullable
    @Override
    public FluidStack getContents() {
        return getFluid();
    }

    @Override
    public int getCapacity() {
        return this.maxCapacity;
    }

    @Override
    public boolean canFill() {
        return this.allowInput && this.amount < this.maxCapacity;
    }

    @Override
    public boolean canDrain() {
        return this.allowOutput && this.amount > 0 && this.fluid != null;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack) {
        return canFill() && (this.fluid == null || fluidStack.getFluid().equals(this.fluid));
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack) {
        return canDrain() && (this.fluid != null && fluidStack.getFluid().equals(this.fluid));
    }

    public float getPercentageFilled() {
        return (((float) amount) / ((float) maxCapacity));
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(this);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] { this };
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!canFillFluidType(resource)) return 0;
        int maxAdded = resource.amount;
        int addable = getMaxAddable(maxAdded);
        if(addable > 0 && this.fluid == null && doFill) {
            setFluid(resource.getFluid());
        }
        if(doFill) {
            addable -= addAmount(addable);
        }
        return addable;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (!canDrainFluidType(resource)) return null;
        return drain(resource.amount, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!canDrain()) return null;
        int maxDrainable = getMaxDrainable(maxDrain);
        if (doDrain) {
            return drain(maxDrainable);
        }
        return new FluidStack(this.fluid, maxDrainable);
    }

    public CompoundNBT writeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt("amt", this.amount);
        tag.putInt("capacity", this.maxCapacity);
        tag.putBoolean("aIn", this.allowInput);
        tag.putBoolean("aOut", this.allowOutput);
        if(this.fluid != null) {
            tag.putString("fluid", this.fluid.getName());
        }
        int[] sides = new int[accessibleSides.size()];
        for (int i = 0; i < accessibleSides.size(); i++) {
            Direction side = accessibleSides.get(i);
            sides[i] = side.ordinal();
        }
        tag.putIntArray("sides", sides);
        return tag;
    }

    public void readNBT(CompoundNBT tag) {
        this.amount = tag.getInt("amt");
        this.maxCapacity = tag.getInt("capacity");
        this.allowInput = tag.getBoolean("aIn");
        this.allowOutput = tag.getBoolean("aOut");
        if(tag.contains("fluid")) {
            this.fluid = FluidRegistry.getFluid(tag.getString("fluid"));
        } else {
            this.fluid = null;
        }
        int[] sides = tag.getIntArray("sides");
        for (int i : sides) {
            this.accessibleSides.add(Direction.values()[i]);
        }
    }

    public static SimpleSingleFluidTank deserialize(CompoundNBT tag) {
        SimpleSingleFluidTank tank = new SimpleSingleFluidTank();
        tank.readNBT(tag);
        return tank;
    }

    private boolean hasHandlerForSide(@Nullable Direction facing) {
        return facing == null || accessibleSides.contains(facing);
    }

    public boolean hasCapability(Capability<?> capability, @Nullable Direction facing) {
        return hasHandlerForSide(facing) && CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == capability;
    }

    public LazyOptional<SimpleSingleFluidTank> getCapability() {
        return LazyOptional.of(() -> this);
    }

}
