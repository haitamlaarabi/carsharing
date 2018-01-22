package org.matsim.contrib.gcs.carsharing.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.gcs.carsharing.core.CarsharingAgent;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleBattery;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehiclePark;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleTrip;
import org.matsim.vehicles.Vehicle;


public class CarsharingVehicleImpl implements CarsharingVehicleMobsim {
	
	private static Logger logger = Logger.getLogger(CarsharingVehicleImpl.class);
	
	private final Vehicle vehicle;
	private final Map<String, Object> attributes;
	CarsharingVehicleBattery battery;
	LinkedList<CarsharingVehicleMobsim> trailer;
	CarsharingVehicleStatus status;
	
	CarsharingVehicleImpl(Vehicle vehicle) {
		this.vehicle = vehicle;
		this.attributes = new HashMap<String, Object>();
		this.trailer = new LinkedList<CarsharingVehicleMobsim>();
		this.status = new CarsharingVehicleStatus();
	}	

	@Override 
	public void reset(int iteration) {
		battery().reset();
		this.trailer.clear();
		this.status = new CarsharingVehicleStatus();
	}
	
	@Override 
	public void dock(CarsharingVehicleMobsim vehicle) {
		this.trailer.offer(vehicle);
	}
	
	@Override
	public Queue<CarsharingVehicleMobsim> undock() {
		Queue<CarsharingVehicleMobsim> q = new LinkedList<CarsharingVehicleMobsim>();
		q.offer(this);
		while(!this.trailer.isEmpty()) {
			q.offer(this.trailer.poll());
		}
		return q;
	}

	@Override 
	public Queue<CarsharingVehicleMobsim> roadTrain() {
		Queue<CarsharingVehicleMobsim> q = new LinkedList<CarsharingVehicleMobsim>();
		q.add(this);
		q.addAll(this.trailer);
		return q;
	}
	
	@Override 
	public void startTrip(CarsharingAgent agent, CarsharingStationMobsim station, double time) {
		for(CarsharingVehicleMobsim v : this.roadTrain()) {
			v.status().setTrip(new CarsharingVehicleTrip(this, agent.getId(), station, time, true));
		}
	}
	
	@Override 
	public void endTrip(CarsharingStationMobsim arrivalStation, double time, String tripstatus) {
		for(CarsharingVehicleMobsim v : this.roadTrain()) {
			v.status().getTrip().finalize(time, arrivalStation, tripstatus);
		}
	}
	
	@Override 
	public void startPark(CarsharingAgent agent, CarsharingStationMobsim station, double time) {
		String id;
		if (agent == null) {
			id = station.getId().toString();
		} else {
			id = agent.getId().toString();
		}
		for(CarsharingVehicleMobsim v : this.roadTrain()) {
			v.status().setPark(new CarsharingVehiclePark(this, id, station, time));
		}
	}
	
	@Override 
	public void endPark(double time) {
		for(CarsharingVehicleMobsim v : this.roadTrain()) {
			v.status().getPark().finalize(true, time);
		}
	}
	
	@Override
	public void drive(double distance, double speed, double freespeed) {
		for(CarsharingVehicleMobsim  v : this.roadTrain()) {
			v.battery().consumeBattery(distance, speed, freespeed);
			v.status().getTrip().increment(distance);
			if(v.battery().getSoC() <= 0) {
				logger.error("[DRIVING-KO] tripId:" + v.status().getTrip().getId()  + " |vehId:" + v.vehicle().getId() + " |soc:" + v.battery().getSoC());
			}
		}
	}

	@Override public String getName(){ return (String) this.attributes.get("NAME"); }
	@Override public String getType() { return (String) this.attributes.get("TYPE"); }
	@Override public void setName(String name){ this.attributes.put("NAME", name); }
	@Override public void setType(String type) { this.attributes.put("TYPE", type); }
	
	@Override public CarsharingVehicleBattery battery() { return this.battery; }
	@Override public Vehicle vehicle() {	return this.vehicle; }
	@Override public CarsharingVehicleStatus status() { return status; }

	@Override
	public Id<Vehicle> getId() {
		return this.vehicle.getId();
	}
	
}
