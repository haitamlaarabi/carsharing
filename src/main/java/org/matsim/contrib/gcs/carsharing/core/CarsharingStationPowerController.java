package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerDistributionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerSourceModel;

public class CarsharingStationPowerController {

	private CarsharingPowerSourceModel powerSource;
	private CarsharingPowerDistributionModel powerDistribution;
	
	public CarsharingStationPowerController(CarsharingPowerSourceModel powerSource, CarsharingPowerDistributionModel powerDistribution) {
		this.powerSource = powerSource;
		this.powerDistribution = powerDistribution;
	}
	
	public double getPower(double time) {
		return powerSource.getPower(time);
	}

	public void distributePower(CarsharingParkingModel parkedVehicles, double now) {
		powerDistribution.charge(parkedVehicles, powerSource, now);
	}

	public CarsharingPowerSourceModel getPowerSource() {
		if(powerSource.getPower(0) == Double.NaN) 
			return null;
		else 
			return powerSource;
	}

	public CarsharingPowerDistributionModel getPowerDistribution() {
		return powerDistribution;
	}

	public void reset() {
		
	}

}
