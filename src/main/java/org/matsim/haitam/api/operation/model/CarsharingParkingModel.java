package org.matsim.haitam.api.operation.model;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.haitam.api.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingParkingModel extends CarsharingOperationModel, Iterable<CarsharingVehicleMobsim> {
		
	boolean isFull();
	int getCapacity();
	void setCapactiy(int capacity);
	int getFleetSize();
	int getParkingSize();
	CarsharingVehicleMobsim[] park(CarsharingVehicleMobsim vehicle);
	CarsharingVehicleMobsim unpark(int numberOfVehicles);
	void reset();
	Collection<CarsharingVehicleMobsim> getAll();
	
}
