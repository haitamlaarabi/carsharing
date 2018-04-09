package org.matsim.contrib.gcs.carsharing.core;

import org.apache.log4j.Logger;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation.BookingRecordWrapper;

public class CarsharingStationDemand {

	private static Logger logger = Logger.getLogger(CarsharingStationDemand.class);
	
	CarsharingBookingStation b;

	public CarsharingStationDemand(CarsharingBookingStation b) {
		b = new CarsharingBookingStation(b);
	}
	
	public CarsharingStationDemand(CarsharingStationMobsim s) {
		b = new CarsharingBookingStation(s);
	}	
	
	public CarsharingStationDemand(CarsharingStation s) {
		b = new CarsharingBookingStation(s);
	}	
	
	
	public boolean push(CarsharingBookingRecord dm) {
		BookingRecordWrapper w = b.new BookingRecordWrapper(dm);
		if(w.isDemand) {
			b.car_availability_tracker -= dm.getNbrOfVeh(); 
		} else {
			b.parking_availability_tracker -= dm.getNbrOfVeh();
		}
		if(b.car_availability_tracker < 0 || b.parking_availability_tracker > b.station.getCapacity()) {
			logger.warn("Availability in station "+ b.station + " is " + b.car_availability_tracker);
		}
		w.car_availability_flag = b.car_availability_tracker;
		w.parking_availability_flag = b.parking_availability_tracker;
		b.activity.add(w);
		b.booking_wrapper.put(dm, w);
		return true;
	}
	
	public int vehicleMinAvailability(int V0, double lb, double up) {
		int min_cav = V0;
		int av = min_cav;
		for(BookingRecordWrapper w : b.activity.subList(b.new BookingRecordWrapper(lb), b.new BookingRecordWrapper(up))) {
			if(w.isDemand) 
				av -= w.record.getNbrOfVeh();
			else if(!w.isDemand) 
				av += w.record.getNbrOfVeh();
			if(av < min_cav) 
				min_cav = av;
		}
		return min_cav;
	}
	
	public int parkingMinAvailability(int V0, double lb, double up) {
		int min_pav = b.station.parking().getCapacity() - V0;
		int pav = min_pav;
		for(BookingRecordWrapper w : b.activity.subList(b.new BookingRecordWrapper(lb), b.new BookingRecordWrapper(up))) {
			if(w.isDemand) 
				pav += w.record.getNbrOfVeh();
			else if(!w.isDemand) 
				pav -= w.record.getNbrOfVeh();
			if(pav < min_pav) 
				min_pav = pav;
		}
		return min_pav;
	}
	
	public CarsharingStationMobsim getStation() {
		return this.b.getStation();
	}
	
	public CarsharingBookingStation getBooking() {
		return this.b;
	}
	

	double coef;
	public double getCoef() {
		return coef;
	}
	public void setCoeff(double coefficient) {
		coef = coefficient;
	}	
	
}
