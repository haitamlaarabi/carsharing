package org.matsim.contrib.gcs.events;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingAgent;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehiclePark;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleTrip;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public abstract class AbstractCarsharingEvent extends Event implements CarsharingDataProvider {

	protected static CoordinateTransformation ct = null;
	protected final Scenario scenario;
	protected final CarsharingManager manager;
	
	StationLog station_log;
	BookLog book_log;
	ParkLog park_log;
	TripLog trip_log;
	
	final String time;
	final String agent_id;
	final String is_operator;
	
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static int year = 2017;
	static int month = 3;
	static int day = 1 ;

	public AbstractCarsharingEvent(double time, Scenario scenario, CarsharingManager manager, CarsharingAgent agent, CarsharingStationMobsim station) {
		super(time);
		this.scenario = scenario;
		if(ct == null) {
			ct = TransformationFactory.getCoordinateTransformation(scenario.getConfig().global().getCoordinateSystem(), TransformationFactory.WGS84);
		}
		this.manager = manager;
		this.station_log = new StationLog(station);
		this.time = String.valueOf(time);
		if(agent != null) {
			this.agent_id = agent.getId();
			this.is_operator = (agent instanceof CarsharingOperatorMobsim? "TRUE" : "FALSE");
		} else {
			this.agent_id = "NA";
			this.is_operator = "NA";
		}
	}

	
	class StationLog {
		final String station_id;
		final String lng;
		final String lat;
		final String parked_at_stat;
		final String capacity;
		StationLog(CarsharingStationMobsim s) {
			if(s != null) {
				this.station_id = String.valueOf(s.facility().getId());
				Coord c = ct.transform(s.facility().getCoord());
				this.lng = String.valueOf(c.getX());
				this.lat = String.valueOf(c.getY());
				this.parked_at_stat = String.valueOf(s.parking().getFleetSize());
				this.capacity = String.valueOf(s.parking().getCapacity());
			} else {
				this.station_id = "NA";
				this.lng = "NA";
				this.lat = "NA";
				this.parked_at_stat = "NA";
				this.capacity = "NA";
			}
		}
	}
	
	class BookLog {
		final String trip_id;
		final String booking_id;
		final String nbr_of_veh;
		final String trip_index;
		
		// src
		final StationLog src_station_log;
		final String src_request_status;
		final String src_distance_act;
		final String src_time_act;
		final String src_distance_drive;
		final String src_time_drive;
		final String src_type;
		final String src_date;
		final String src_offset;
		
		// dst
		final StationLog dst_station_log;
		final String dst_request_status;
		final String dst_distance_act;
		final String dst_time_act;
		final String dst_distance_drive;
		final String dst_time_drive;
		final String dst_type;
		final String dst_date;
		final String dst_offset;
		
		final String comment;
		
		
		BookLog(CarsharingBookingRecord r) {
			this.trip_index = String.valueOf(r.getDemand().getTripIndex());
			
			this.trip_id = r.trip();
			this.booking_id = r.getId();
			this.nbr_of_veh = String.valueOf(r.getNbrOfVeh());
			if(r.vehicleOffer()) {
				this.src_request_status = "SUCCESS";
			} else {
				this.src_request_status = "FAILURE";
			}
			if(r.parkingOffer()) {
				this.dst_request_status = "SUCCESS";
			} else {
				this.dst_request_status = "FAILURE";
			}
			if(r.getRelatedOffer() != null) {
				this.src_station_log = new StationLog(r.getOriginStation());
				this.src_distance_act = String.valueOf(r.getRelatedOffer().getAccess().getDistance());
				this.src_time_act = String.valueOf(r.getRelatedOffer().getAccess().getTravelTime());
				this.src_distance_drive = String.valueOf(r.getRelatedOffer().getDrive().getDistance());
				this.src_time_drive = String.valueOf(r.getRelatedOffer().getDrive().getRentalTime());
				this.src_offset = String.valueOf(manager.getConfig().getInteractionOffset());
				
				this.dst_station_log = new StationLog(r.getDestinationStation());
				this.dst_distance_act = String.valueOf(r.getRelatedOffer().getEgress().getDistance());
				this.dst_time_act = String.valueOf(r.getRelatedOffer().getEgress().getTravelTime());
				this.dst_distance_drive = String.valueOf(r.getRelatedOffer().getDrive().getDistance());
				this.dst_time_drive = String.valueOf(r.getRelatedOffer().getDrive().getRentalTime());
				this.dst_offset = String.valueOf(manager.getConfig().getInteractionOffset());
			} else {
				this.src_station_log = new StationLog(null);
				this.src_distance_act = "NA";
				this.src_time_act = "NA";
				this.src_distance_drive = "NA";
				this.src_time_drive = "NA";
				this.src_offset = "NA";
				
				this.dst_station_log = new StationLog(null);
				this.dst_distance_act = "NA";
				this.dst_time_act = "NA";
				this.dst_distance_drive = "NA";
				this.dst_time_drive = "NA";
				this.dst_offset = "NA";
			}
			this.src_type = "START";
			this.dst_type = "END";
			this.src_date = String.valueOf(r.getDepartureTime());
			this.dst_date = String.valueOf(r.getArrivalTime());
			this.comment = r.getComment();
		}
	}
	
	class ParkLog {
		final String park_id;
		final String vehicle_id;
		final String vehicle_fuel;
		final String chargin_duration;
		
		ParkLog(CarsharingVehiclePark p) {
			if(p != null) {
				this.chargin_duration = String.valueOf(p.getChargingDuration());
				this.park_id = p.getId();
				this.vehicle_id = String.valueOf(p.getVehicle().vehicle().getId());
				int fuel_percentage = (int) (100 * p.getVehicle().battery().getSoC()/p.getVehicle().battery().getSafeBatteryCapacity());
				this.vehicle_fuel = String.valueOf(fuel_percentage);
			} else {
				this.vehicle_id = "NA";
				this.vehicle_fuel = "NA";
				this.park_id = "NA";
				this.chargin_duration = "NA";
			}
		}
	}
	
	class TripLog {
		String trip_id;
		String trip_distance;
		String trip_time;
		String rental_cost; 
		String vehicle_id;
		String vehicle_fuel;
		String nbre_veh;
		String booking_id;
		
		TripLog(String trip_id) {
			this.vehicle_id = "NA";
			this.trip_id = trip_id;
			this.trip_distance = "NA";
			this.rental_cost = "NA";
			this.vehicle_fuel = "NA";
		}
		
		TripLog(CarsharingVehicleTrip t, CarsharingVehicleMobsim v, int nbre_veh) {
			if(t != null) {
				this.trip_id = String.valueOf(t.getId());
				this.trip_distance = String.valueOf(t.getTravelDistance());
				this.trip_time = String.valueOf(t.getTravelTime());
				this.rental_cost = String.valueOf(t.getRentalCost());
				this.vehicle_id = String.valueOf(v.vehicle().getId());
				int fuel_percentage = (int) (100 * v.battery().getSoC()/v.battery().getSafeBatteryCapacity());
				this.vehicle_fuel = String.valueOf(fuel_percentage);
			} else {
				if(v != null) {
					this.vehicle_id = v.vehicle().getId().toString();
					this.trip_id = station_log.station_id+"@0";
					this.trip_distance = "0";
					this.trip_time = "0";
					this.rental_cost = "0";
					int fuel_percentage = (int) (100 * v.battery().getSoC()/v.battery().getSafeBatteryCapacity());
					this.vehicle_fuel = String.valueOf(fuel_percentage);
				} else {
					this.vehicle_id = "NA";
					this.trip_id = "NA";
					this.trip_distance = "NA";
					this.trip_time = "NA";
					this.rental_cost = "NA";
					this.vehicle_fuel = "NA";
				}
			}
			this.nbre_veh = String.valueOf(nbre_veh);
		}
	}

}
