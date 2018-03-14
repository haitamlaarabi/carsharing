package org.matsim.contrib.gcs.carsharing.core;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

public interface CarsharingStation {

	//Map<Id<Vehicle>, CarsharingVehicle> initialFleet();
	void addToDeployment(CarsharingVehicle v);
	Collection<CarsharingVehicle> deployment();
	ActivityFacility facility();
	Id<ActivityFacility> getId();
	
	String getType();
	String getName();
	int getCapacity();
	
	void setType(String t);
	void setName(String n);
	void setCapacity(int c);
	
}
