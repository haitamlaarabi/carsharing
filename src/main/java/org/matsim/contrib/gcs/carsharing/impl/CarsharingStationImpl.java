package org.matsim.contrib.gcs.carsharing.impl;

import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.gcs.carsharing.core.CarsharingAgent;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationPowerController;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicle;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleTrip;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;


public class CarsharingStationImpl implements CarsharingStationMobsim {
	
	private static Logger logger = Logger.getLogger(CarsharingStationImpl.class);
	
	HashMap<Id<Vehicle>, CarsharingVehicle> fleet;
	private final ActivityFacility facility;
	CarsharingParkingModel parking;
	CarsharingStationPowerController powerController;
	

	
	public CarsharingStationImpl(ActivityFacility facility) {
		this.facility = facility;
		this.fleet = new HashMap<Id<Vehicle>, CarsharingVehicle>();
		this.parking = null;
		this.powerController = null;
	}
	
	@Override 
	public boolean dropoff(CarsharingAgent agent, CarsharingVehicleMobsim leadVehicle, double time) {
		// Drop-off at time 0, refers to initial deployment of the fleet
		if(time == 0.0) {
			if(parking().isFull() ) {
				throw new RuntimeException("Station Full... Initial deployment needs to take into consideration the predefined capacity of the station");
			}
			leadVehicle.startPark(agent, this, time);
			parking().park(leadVehicle);
			return true;
		}
		// Otherwise
		synchronized(parking()) {
			// CHARGING
			POWER(time);
			// PARKING
			if(leadVehicle.battery().isDead()) {
				leadVehicle.endTrip(this, time, CarsharingVehicleTrip.STATUS_ENDTRIP_NOENERGY);
			} else {
				CarsharingVehicleMobsim[] carsharings = parking().park(leadVehicle);
				if (carsharings == null) {
					leadVehicle.endTrip(this, time, CarsharingVehicleTrip.STATUS_ENDTRIP_NOSPACE);
				} else {
					for(CarsharingVehicleMobsim c : carsharings) {
						c.endTrip(this, time, CarsharingVehicleTrip.STATUS_ENDTRIP_OK);
						c.startPark(agent, this, time);
					}
					return true;
				}
			}
		}

		return false;
	}
	
	@Override 
	public CarsharingVehicleMobsim pickup(CarsharingAgent agent, int nbrVehicle, double time) {
		synchronized(parking()) {
			// CHARGING
			POWER(time);
			
			// PARKING
			CarsharingVehicleMobsim leadVehicle = parking().unpark(nbrVehicle);
			if(leadVehicle != null) {
				leadVehicle.endPark(time);
				leadVehicle.startTrip(agent, this, time);
				return leadVehicle;
			} else {
				logger.warn("[PICKUP-KO] staId:" + this.getId() + " |agentId: " + agent.getId());
			}
		}
		return null;
	}
	

	private void POWER(double now) {
		if(this.powerController != null) {
			this.powerController.distributePower(parking(), now);
		}
	}

	
	@Override 
	public void reset(int iteration) {
		parking().reset();
		powerController.reset();
		for(CarsharingVehicle v: deployment()) {
			this.dropoff(null, (CarsharingVehicleMobsim)v, 0.0);
		}
	}
	

	@Override public int getCapacity() { return (int) this.facility.getCustomAttributes().get("CAPACITY"); }
	@Override public String getType() {	return (String) this.facility.getCustomAttributes().get("TYPE"); }
	@Override public String getName() { return (String) this.facility.getCustomAttributes().get("NAME"); }
	
	@Override public CarsharingStationPowerController powerController() { return this.powerController; }
	@Override public CarsharingParkingModel parking() {	return this.parking; }
	@Override public ActivityFacility facility() { return this.facility; }
	
	@Override public void setType(String t) { this.facility.getCustomAttributes().put("TYPE", t); }
	@Override public void setName(String n) { this.facility.getCustomAttributes().put("NAME", n); }
	@Override public void setCapacity(int c) { this.facility.getCustomAttributes().put("CAPACITY", c); }
	
	@Override public String toString() { return this.facility.getId().toString(); }
	@Override public boolean equals(Object o) {
		if(o instanceof CarsharingStationImpl) {
			return ((CarsharingStationImpl)o).getId().toString().compareTo(this.getId().toString()) == 0;
		}
		return false;
	}

	@Override
	public Id<ActivityFacility> getId() {
		return this.facility.getId();
	}

	@Override
	public void addToDeployment(CarsharingVehicle v) {
		if(this.fleet.size() >= this.getCapacity()) {
			throw new RuntimeException("station is already full " + this.facility.getId());
		}
		this.fleet.put(v.getId(), v);
		
	}

	@Override
	public Collection<CarsharingVehicle> deployment() {
		return this.fleet.values();
	}
		
}
