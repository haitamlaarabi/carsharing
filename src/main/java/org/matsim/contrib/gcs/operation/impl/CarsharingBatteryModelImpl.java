/**
 * As part of:
 * A Generic Software Framework for car-Sharing Modelling based on a Large-Scale Multi-Agent Traffic Simulation Platform
 * 
 * @author haitam
 *
 */

package org.matsim.contrib.gcs.operation.impl;

import java.util.Random;

import org.matsim.contrib.gcs.operation.model.CarsharingBatteryModel;
import org.matsim.core.gbl.MatsimRandom;



public class CarsharingBatteryModelImpl implements CarsharingBatteryModel {
	
	protected final double batteryCapacity_Joules = 4*1000*3600; // Joules (or 4 kWh)
	protected final double batteryThreshold_PerCent = 0.2; // 20%
	protected final double batteryInitialSoc_PerCent = 0.7; // 70%
	protected final double batteryBalancingThreshold_PerCent = 0.5; // 50%

	@Override
	public double theoreticalCapacity() {
		return batteryCapacity_Joules;
	}

	@Override
	public double safeCapacity() {
		return batteryCapacity_Joules - batteryCapacity_Joules * 0.01;
	}

	@Override
	public double calculateCriticalSocThreshold() {
		return batteryCapacity_Joules * batteryThreshold_PerCent;
	}

	/**
	 * calculateInitialSoc
	 * It calculates a random initial value for SOC within interval of (batteryInitialSoc_PerCent - 0.1 < % < batteryInitialSoc_PerCent + 0.1)
	 */
	@Override
	public double calculateInitialSoc() {
		Random rng = MatsimRandom.getRandom();
		double minValue = batteryCapacity_Joules * (batteryInitialSoc_PerCent - 0.1);
		double maxValue = batteryCapacity_Joules * (batteryInitialSoc_PerCent + 0.1);
		double rng_capacity = minValue + rng.nextDouble() * (maxValue - minValue);
		return rng_capacity;
	}
	
	/**
	 * calculateChargingRate
	 * It assumes that battery absorbs all the power provided
	 * @param SoC
	 * @param powerInJoules
	 * @param durationInSeconds
	 */
	@Override
	public double calculateChargingRateInJoule(double SoC, double powerInJoules, double durationInSeconds) {
		double energyRequired = safeCapacity() - SoC;
		double energyToCharge = powerInJoules * durationInSeconds;
		return Math.min(energyToCharge, energyRequired);
	}
	
	
	/**
	 * 
	 */
	@Override
	public double getBalancingEnergy(double SoC) {
		return SoC - (batteryCapacity_Joules * batteryBalancingThreshold_PerCent);
	}
	

	@Override
	public double getBalancingEnergy(double SoC, double durationInSeconds) {
		double maxEnergyToBalance = getBalancingEnergy(SoC);
		double batteryPower = getBalancingPower();
		double energyToBalance = batteryPower * durationInSeconds;
		return Math.min(maxEnergyToBalance, energyToBalance);
	}
	
	/**
	 * 
	 */
	@Override
	public double getBalancingPower() {
		return batteryCapacity_Joules * 3600;
	}
	
	
	

	/**
	 * calculateBalancingRate
	 * The balancing power is computed directly from the battery capacity.
	 * @param SoC
	 * @param durationInSeconds
	 */
	/*@Override
	public PowerPerDurationLabel calculateBalancingRate(double SoC, double durationInSeconds) {
		PowerPerDurationLabel balance;
		double batteryPower = getBalancingPower();
		double energyToBalance = batteryPower * durationInSeconds;
		double maxEnergyToBalance = getBalancingEnergy(SoC);
		
		if(maxEnergyToBalance > 0 && energyToBalance <= maxEnergyToBalance) {
			balance = new PowerPerDurationLabel(batteryPower, durationInSeconds);
		} else if (energyToBalance > maxEnergyToBalance) {
			double maxDuration = maxEnergyToBalance / batteryPower;
			balance = new PowerPerDurationLabel(batteryPower, maxDuration);
		} else { // maxEnergyToBalance <= 0
			balance = new PowerPerDurationLabel(0.0, 0.0);
		}
		
		return balance;
	}*/

}
