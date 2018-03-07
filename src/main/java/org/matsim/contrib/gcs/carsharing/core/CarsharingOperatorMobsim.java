package org.matsim.contrib.gcs.carsharing.core;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;


public interface CarsharingOperatorMobsim extends CarsharingAgent {

	CarsharingOperatorChoiceModel decision();
	
	CarsharingStationMobsim getLocation();
	CarsharingVehicleMobsim getVehicle();
	void setLocation(CarsharingStationMobsim location);
	void setVehicle(CarsharingVehicleMobsim vehicle);
	
	int getMaxRoadtrainSize();
	boolean available();
	
	ArrayList<CarsharingRelocationTask> getAllTasks();
	
	CarsharingRelocationTask getTask();
	void addTask(CarsharingRelocationTask task);
	void addManyTasks(Collection<CarsharingRelocationTask> tasks);
	public CarsharingRelocationTask endTask();
	
}
