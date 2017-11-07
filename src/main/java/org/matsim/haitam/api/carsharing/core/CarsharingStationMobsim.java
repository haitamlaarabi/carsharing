package org.matsim.haitam.api.carsharing.core;

import org.matsim.haitam.api.operation.model.CarsharingParkingModel;

public interface CarsharingStationMobsim extends CarsharingStation {

	CarsharingParkingModel parking();
	CarsharingStationPowerController powerController();
	public boolean dropoff(CarsharingAgent agent, CarsharingVehicleMobsim leadVehicle, double time);
	public CarsharingVehicleMobsim pickup(CarsharingAgent agent, int nbrVehicle, double time);
	public void reset(int iteration);

}
