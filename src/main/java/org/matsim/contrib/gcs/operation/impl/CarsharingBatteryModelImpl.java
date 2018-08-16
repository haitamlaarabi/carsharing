/**
 * As part of:
 * A Generic Software Framework for car-Sharing Modelling based on a Large-Scale Multi-Agent Traffic Simulation Platform
 * 
 * @author haitam
 *
 */

package org.matsim.contrib.gcs.operation.impl;

import org.matsim.contrib.gcs.operation.model.CarsharingBatteryModel;



public class CarsharingBatteryModelImpl implements CarsharingBatteryModel {
	
	protected final double CRITICAL_THRESHOLD = 0.2; // 20%
	protected final double BALANCING_THRESHOLD = 0.5; // 50%
	protected final double capacity;
	
	public CarsharingBatteryModelImpl() {
		this(4*1000*3600); // Joules (or 4 kWh)
	}
	
	public CarsharingBatteryModelImpl(double capacity) {
		this.capacity = capacity;
	}

	@Override
	public double theoreticalCapacity() {
		return this.capacity; 
	}

	@Override
	public double safeCapacity() {
		return theoreticalCapacity() - theoreticalCapacity()*0.01;
	}

	@Override
	public double calculateCriticalSocThreshold() {
		return safeCapacity()*CRITICAL_THRESHOLD;
	}

	/**
	 * calculateInitialSoc
	 * It calculates a random initial value for SOC within interval of (batteryInitialSoc_PerCent - 0.1 < % < batteryInitialSoc_PerCent + 0.1)
	 */
	@Override
	public double calculateInitialSoc() {
		return safeCapacity();
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

	@Override
	public double getBalancingEnergy(double SoC) {
		return SoC-(safeCapacity()*BALANCING_THRESHOLD);
	}

	@Override
	public double getBalancingEnergy(double SoC, double durationInSeconds) {
		double maxEnergyToBalance = getBalancingEnergy(SoC);
		double batteryPower = getBalancingPower();
		double energyToBalance = batteryPower * durationInSeconds;
		return Math.min(maxEnergyToBalance, energyToBalance);
	}

	@Override
	public double getBalancingPower() {
		return safeCapacity() * 3600;
	}

}
