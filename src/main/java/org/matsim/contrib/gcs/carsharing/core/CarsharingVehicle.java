package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public interface CarsharingVehicle {

	String getType();
	String getName();
	void setName(String name);
	void setType(String type);
	
	Vehicle vehicle();
	Id<Vehicle> getId();
	
	
}
