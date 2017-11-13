package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;

public interface CarsharingStationMobsim extends CarsharingStation {

	CarsharingParkingModel parking();
	CarsharingStationPowerController powerController();
	public boolean dropoff(CarsharingAgent agent, CarsharingVehicleMobsim leadVehicle, double time);
	public CarsharingVehicleMobsim pickup(CarsharingAgent agent, int nbrVehicle, double time);
	public void reset(int iteration);

}
