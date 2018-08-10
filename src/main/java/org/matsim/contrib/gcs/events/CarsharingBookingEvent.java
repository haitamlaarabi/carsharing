package org.matsim.contrib.gcs.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;

public class CarsharingBookingEvent extends AbstractCarsharingEvent {


	public CarsharingBookingEvent(
			double time, 
			Scenario scenario, 
			CarsharingManager manager, 
			CarsharingDemand demand,
			CarsharingBookingRecord bookingRecord) {
		super(time, scenario, manager, bookingRecord.getDemand().getAgent(), null);
		
		this.book_log = new BookLog(bookingRecord);
		
	}
	
	private Map<String, String> getCarBookingLog() {
		Map<String, String> logRow = new HashMap<String, String>();
		logRow.put("booking.time", this.time);
		logRow.put("date", this.book_log.src_date);
		logRow.put("date.veh", this.book_log.src_date_veh);
		logRow.put("customer.id", this.agent_id);
		
		logRow.put("booking.id", this.book_log.booking_id);				
		logRow.put("trip.id", this.book_log.trip_id);
		logRow.put("nbr.vehicle", this.book_log.nbr_of_veh);
		logRow.put("trip.index", this.book_log.trip_index);
		
		logRow.put("type", this.book_log.src_type);
		logRow.put("request.status", this.book_log.src_request_status);
		logRow.put("distance.act", this.book_log.src_distance_act);
		logRow.put("time.act", this.book_log.src_time_act);
		logRow.put("distance.drive", this.book_log.src_distance_drive);
		logRow.put("time.drive", this.book_log.src_time_drive);
		logRow.put("time.rental", this.book_log.src_time_rental);
		
		logRow.put("station.id", this.book_log.src_station_log.station_id);
		logRow.put("lng", this.book_log.src_station_log.lng);
		logRow.put("lat", this.book_log.src_station_log.lat);
		logRow.put("parked.at.stat", this.book_log.src_station_log.parked_at_stat);
		logRow.put("capacity", this.book_log.src_station_log.capacity);
		
		logRow.put("facility.lng", this.book_log.src_lng);
		logRow.put("facility.lat", this.book_log.src_lat);
		
		logRow.put("status", this.book_log.status);
		
		return logRow;
	}
	
	private Map<String, String> getParkBookingLog() {
		Map<String, String> logRow = new HashMap<String, String>();
		logRow.put("booking.time", this.time);
		logRow.put("date", this.book_log.dst_date);
		logRow.put("date.veh", this.book_log.dst_date_veh);
		logRow.put("customer.id", this.agent_id);
		
		logRow.put("trip.id", this.book_log.trip_id);
		logRow.put("nbr.vehicle", this.book_log.nbr_of_veh);
		logRow.put("booking.id", this.book_log.booking_id);	
		logRow.put("trip.index", this.book_log.trip_index);
		
		logRow.put("type", this.book_log.dst_type);
		logRow.put("request.status", this.book_log.dst_request_status);
		logRow.put("distance.act", this.book_log.dst_distance_act);
		logRow.put("time.act", this.book_log.dst_time_act);
		logRow.put("distance.drive", this.book_log.dst_distance_drive);
		logRow.put("time.drive", this.book_log.dst_time_drive);
		logRow.put("time.rental", this.book_log.dst_time_rental);
		
		logRow.put("station.id", this.book_log.dst_station_log.station_id);
		logRow.put("lng", this.book_log.dst_station_log.lng);
		logRow.put("lat", this.book_log.dst_station_log.lat);
		logRow.put("parked.at.stat", this.book_log.dst_station_log.parked_at_stat);
		logRow.put("capacity", this.book_log.dst_station_log.capacity);
		
		logRow.put("facility.lng", this.book_log.dst_lng);
		logRow.put("facility.lat", this.book_log.dst_lat);
		
		logRow.put("status", this.book_log.status);
		
		return logRow;
	}
	
	
	@Override
	public Collection<Map<String, String>> getLogRows(double time) {
		ArrayList<Map<String, String>> rows = new ArrayList<Map<String, String>>();
		rows.add(getCarBookingLog());
		rows.add(getParkBookingLog());
		return rows;
	}

	@Override
	public String getLogFile() {
		return manager.getConfig().getBookingLogFile();
	}
	
	@Override
	public String getLogType() {
		return "failure_events";
	}

	@Override
	public String getModuleName() {
		return getEventType();
	}
	
	@Override
	public String getEventType() {
		return this.getClass().getSimpleName();
	}
	

	@Override
	public void reset(int iteration) {
	}

}
