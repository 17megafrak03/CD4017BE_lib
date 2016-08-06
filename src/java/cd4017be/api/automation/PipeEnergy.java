/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAutomation;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.util.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author CD4017BE
 */
public class PipeEnergy implements IEnergyAccess
{
	public boolean update;
	public byte sideCfg;
	public float Ucap;
	public float[] Iind;
	public final int Umax;
	public final float Zcond;

	public PipeEnergy(int Umax, float Rcond) {
		this.Umax = Umax;
		this.Zcond = 1F + Rcond;
		sideCfg = 0x3f;
		Iind = new float[]{0, 0, 0};
	}

	public PipeEnergy connect(byte c) {
		sideCfg = c;
		return this;
	}

	public float getEnergy(float U, float R) {
		if (R < 1) R = 1;
		return (Ucap * Ucap - U * U) / R;
	}
	
	@Override
	public float getStorage() {
		return Ucap * Ucap;
	}

	@Override
	public float getCapacity() {
		return (float)Umax * (float)Umax;
	}

	@Override
	public float addEnergy(float e) {
		float d = Ucap * Ucap; e += d;
		float m = (float)Umax * (float)Umax;
		if (e < 0) {
			e = 0;
			Ucap = 0;
		} else if (e > m) {
			e = m;
			Ucap = Umax;
		} else {
			Ucap = (float)Math.sqrt(e);
			if (Float.isNaN(Ucap)) Ucap = 0F;
		}
		return e - d;
	}

	public void readFromNBT(NBTTagCompound nbt, String name) {
		sideCfg = nbt.getByte(name + "Cfg");
		Ucap = nbt.getFloat(name + "U");
		Iind[0] = nbt.getFloat(name + "Iy");
		Iind[1] = nbt.getFloat(name + "Iz");
		Iind[2] = nbt.getFloat(name + "Ix");
	}

	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setByte(name + "Cfg", sideCfg);
		nbt.setDouble(name + "U", Ucap);
		nbt.setDouble(name + "Iy", Iind[0]);
		nbt.setDouble(name + "Iz", Iind[1]);
		nbt.setDouble(name + "Ix", Iind[2]);
	}

	public boolean isConnected(int s) {
		return (sideCfg >> s & 1) != 0;
	}

	public void update(ModTileEntity tile) {
		PipeEnergy energy;
		for (byte i = 0, s = 0; i < 3; i++, s += 2) 
			if ((sideCfg >> s & 1) != 0) {
				TileEntity te = Utils.getTileOnSide(tile, s);
				if (te != null && te.hasCapability(EnergyAutomation.ELECTRIC_CAPABILITY, EnumFacing.VALUES[s | 1])) {
					energy = te.getCapability(EnergyAutomation.ELECTRIC_CAPABILITY, EnumFacing.VALUES[s | 1]);
					float ii = (Ucap - energy.Ucap) / Zcond;
					float ud = (ii + Iind[i]) * 0.5F;
					Ucap -= ud;
					energy.Ucap += ud;
					Iind[i] = ii;
				}
		}
		if (Ucap > Umax) Ucap = Umax;
	}

	public static String[] getEnergyInfo(float U1, float U0, float R) {
		float I = (U1 - U0) / R;
		float P = (U1 + U0) * I;
		return TooltipInfo.format("gui.cd4017be.energyFlow", P / 1000F, I).split("\n");
		//return new String[]{"Power:", String.format("%.1f kW", P / 1000F), String.format("@ %.0f A", I)};
	}

}
