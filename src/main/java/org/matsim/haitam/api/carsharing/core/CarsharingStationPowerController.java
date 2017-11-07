package org.matsim.haitam.api.carsharing.core;

import org.matsim.haitam.api.operation.model.CarsharingParkingModel;
import org.matsim.haitam.api.operation.model.CarsharingPowerDistributionModel;
import org.matsim.haitam.api.operation.model.CarsharingPowerSourceModel;

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
