package org.matsim.contrib.gcs.operation.model;

import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingPowerDistributionModel extends CarsharingOperationModel {
	
	public void charge(CarsharingParkingModel vehicles, CarsharingPowerSourceModel powerSourceModel, double time);
	public String getName(); 
}
