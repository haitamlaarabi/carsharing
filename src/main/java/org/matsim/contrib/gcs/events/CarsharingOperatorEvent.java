package org.matsim.contrib.gcs.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingAgent;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;


public class CarsharingOperatorEvent extends AbstractCarsharingEvent {

	
	final TripLog trip;
	final String booking_id;
	final String type;
	
	public CarsharingOperatorEvent(double time, Scenario scenario, CarsharingManager manager, 
			CarsharingAgent agent, 
			CarsharingStationMobsim station, 
			CarsharingRelocationTask task) {
		super(time, scenario, manager, agent, station);
		
		this.trip = new TripLog(null, null, 0);
		trip.nbre_veh = "0";
		trip.trip_distance = String.valueOf(task.getDistance());
		trip.trip_time = String.valueOf(task.getTravelTime());
		this.type = String.valueOf(task.getType());
		this.booking_id = "NA";
	}
	
	@Override
	public String getEventType() {
		return this.getClass().getSimpleName();
	}
	
	
	// LOGGING
	@Override
	public Collection<Map<String, String>> getLogRows(double time) {
		ArrayList<Map<String, String>> rows = new ArrayList<Map<String, String>>();
		Map<String, String> logRow = new HashMap<String, String>();
		logRow.put("vehicle.id", this.trip.vehicle_id);
		logRow.put("fuel", this.trip.vehicle_fuel);
		logRow.put("global.id", this.trip.trip_id);
		logRow.put("trip.distance", this.trip.trip_distance);
		logRow.put("trip.time", this.trip.trip_time);
		logRow.put("rental.cost", this.trip.rental_cost);
		logRow.put("type", this.type);
		logRow.put("booking.id", this.booking_id);				
		logRow.put("customer.id", this.agent_id);
		logRow.put("station.id", this.station_log.station_id);
		logRow.put("lng", this.station_log.lng);
		logRow.put("lat", this.station_log.lat);
		logRow.put("parked.at.stat", this.station_log.parked_at_stat);
		logRow.put("capacity", this.station_log.capacity);
		logRow.put("date", this.time);
		logRow.put("operator", this.is_operator);
		rows.add(logRow);
		return rows;
	}

	
	@Override
	public String getLogFile() {
		return manager.getConfig().getTripsLogFile();
	}
	
	@Override
	public String getLogType() {
		return "trips_events";
	}

	@Override
	public String getModuleName() {
		return getEventType();
	}

	@Override
	public void reset(int iteration) {
	}
	

}
