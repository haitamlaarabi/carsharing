package org.matsim.contrib.gcs.operation.model;

import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingBatteryModel extends CarsharingOperationModel {
	
	public abstract double theoreticalCapacity();
	
	public abstract double safeCapacity();
	
	public abstract double calculateCriticalSocThreshold();
	
	public abstract double calculateInitialSoc();
	
	
	// calculate charging rate and duration required for charging
	public abstract double calculateChargingRateInJoule(double SoC, double powerInJoules, double durationInSeconds);
	
	// calculate the amount of energy to be balanced 
	//public abstract PowerPerDurationLabel calculateBalancingRate(double SoC, double durationInSeconds);
	
	// > 0 if surplus, < 0 if shortage,  == 0 if else
	public abstract double getBalancingEnergy(double SoC);
	
	// > 0 if available energy, <= 0 if shortage
	public abstract double getBalancingEnergy(double SoC, double durationInSeconds);
	
	public abstract double getBalancingPower();
	
	
	/*public static class PowerPerDurationLabel {
		private double power;
		private double duration;
		public PowerPerDurationLabel(double power, double duration){
			this.power = power;
			this.duration = duration;
		}
		public double getPower() { return this.power; }
		public double getDuration() { return this.duration; } 
	}*/

}
