package org.matsim.contrib.gcs.carsharing.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;


public class CarsharingOperatorImpl implements CarsharingOperatorMobsim {
	
	
	// ********************************************************
	// ********************************************************
	Person person;
	CarsharingOperatorChoiceModel decisionEngine;
	CarsharingStationMobsim initialLocation;
	int trainSize;
	
	private CarsharingStationMobsim location;
	private ArrayList<CarsharingRelocationTask> tasks;
	private CarsharingVehicleMobsim vehicle;
	private int index;

	public CarsharingOperatorImpl(Person person) {
		this.person = person;
		this.initialLocation = location;
		this.tasks = new ArrayList<CarsharingRelocationTask>();
		this.vehicle = null;
	}
	
	@Override
	public int getMaxRoadtrainSize() {
		return this.trainSize;
	}

	@Override 
	public String getId() {	
		return person.getId().toString(); 
	}
	
	@Override 
	public Person getPerson() { 
		return person; 
	}

	
	@Override
	public void reset(int iteration) {
		this.tasks.clear();
		this.index = 0;
		this.location = this.initialLocation;
	}

	@Override
	public CarsharingStationMobsim getLocation() {
		return this.location;
	}
	
	@Override
	public void setLocation(CarsharingStationMobsim location) {
		this.location = location;
	}
	
	@Override
	public boolean available() {
		return this.getTask() == null;
	}

	@Override
	public CarsharingRelocationTask getTask() {
		if(index >= this.tasks.size()) 
			return null;
		return this.tasks.get(index);
	}

	@Override
	public CarsharingRelocationTask endTask() {
		CarsharingRelocationTask r = this.getTask();
		if(r != null) index++;
		return r;
	}

	@Override
	public ArrayList<CarsharingRelocationTask> getAllTasks() {
		return this.tasks;
	}

	@Override
	public void addTask(CarsharingRelocationTask task) {
		this.tasks.add(task);
	}

	@Override
	public CarsharingOperatorChoiceModel decision() {
		return this.decisionEngine;
	}

	@Override
	public CarsharingVehicleMobsim getVehicle() {
		return this.vehicle;
	}

	@Override
	public void setVehicle(CarsharingVehicleMobsim vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	public void addManyTasks(Collection<CarsharingRelocationTask> tasks) {
		this.tasks.addAll(tasks);
	}
	
	@Override
	public String toString() {
		return this.getId();
	}

}
