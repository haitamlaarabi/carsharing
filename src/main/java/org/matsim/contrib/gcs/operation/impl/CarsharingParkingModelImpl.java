package org.matsim.contrib.gcs.operation.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;

public class CarsharingParkingModelImpl implements CarsharingParkingModel {
	
	final Queue<CarsharingVehicleMobsim> vehicles;
	int capacity;
	
	public CarsharingParkingModelImpl() {
		super();
		this.vehicles = new LinkedList<CarsharingVehicleMobsim>();
	}

	@Override
	public Iterator<CarsharingVehicleMobsim> iterator() {
		return vehicles.iterator();
	}

	@Override
	public boolean isFull() {
		return vehicles.size() == this.capacity;
	}

	@Override
	public int getCapacity() {
		return this.capacity;
	}

	@Override
	public void setCapactiy(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public int getFleetSize() {
		return this.vehicles.size();
	}

	@Override
	public CarsharingVehicleMobsim[] park(CarsharingVehicleMobsim vehicle) {
		int freeSpace = this.capacity - this.vehicles.size();
		CarsharingVehicleMobsim[] parkedvehicles = null;
		if(freeSpace >= vehicle.roadTrain().size()) {
			Queue<CarsharingVehicleMobsim> roadtrain = vehicle.undock();
			parkedvehicles = roadtrain.toArray(new CarsharingVehicleMobsim[0]);
			while(!roadtrain.isEmpty()) {
				this.vehicles.add(roadtrain.poll());
			}
		}
		return parkedvehicles;
	}

	@Override
	public CarsharingVehicleMobsim unpark(int numberOfVehicles) {
		CarsharingVehicleMobsim vehicle = null;
		if(this.vehicles.size() >= numberOfVehicles) {
			while(numberOfVehicles > 0) {
				if(vehicle == null) {
					vehicle = this.vehicles.poll();
				} else {
					vehicle.dock(this.vehicles.poll());
				}
				numberOfVehicles--;
			}
		}
		return vehicle;
	}

	@Override
	public void reset() {
		this.vehicles.clear();
	}

	@Override
	public int getParkingSize() {
		return this.capacity - this.vehicles.size();
	}

	@Override
	public Collection<CarsharingVehicleMobsim> getAll() {
		return this.vehicles;
	}

}
