package org.matsim.haitam.api.operation.model;

import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingPowerSourceModel extends CarsharingOperationModel {
	
	public double getPower(double time);
	public String getName();
	
}
