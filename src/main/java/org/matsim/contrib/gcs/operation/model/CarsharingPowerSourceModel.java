package org.matsim.contrib.gcs.operation.model;

import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingPowerSourceModel extends CarsharingOperationModel {
	
	public double getPower(double time);
	public String getName();
	
}
