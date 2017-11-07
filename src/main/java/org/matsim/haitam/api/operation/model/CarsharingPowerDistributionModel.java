package org.matsim.haitam.api.operation.model;

import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingPowerDistributionModel extends CarsharingOperationModel {
	
	public void charge(CarsharingParkingModel vehicles, CarsharingPowerSourceModel powerSourceModel, double time);
	public String getName(); 
}
