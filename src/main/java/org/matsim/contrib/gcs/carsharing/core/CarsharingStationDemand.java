package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation.BookingRecordWrapper;

public class CarsharingStationDemand {

	CarsharingBookingStation b;
	double coef;

	public CarsharingStationDemand(CarsharingBookingStation b) {
		b = new CarsharingBookingStation(b);
	}
	
	public CarsharingStationDemand(CarsharingStationMobsim s) {
		b = new CarsharingBookingStation(s);
	}	
	
	public CarsharingStationDemand(CarsharingStation s) {
		b = new CarsharingBookingStation((CarsharingStationMobsim)s);
	}	
	
	
	public boolean push(CarsharingBookingRecord dm) {
		BookingRecordWrapper w = b.new BookingRecordWrapper(dm);
		if(w.isDemand) {
			b.car_availability_tracker -= dm.getNbrOfVeh();
			b.park_availability_tracker += dm.getNbrOfVeh();
		} else {
			b.car_availability_tracker += dm.getNbrOfVeh();
			b.park_availability_tracker -= dm.getNbrOfVeh();
		} 
		w.status = true;
		w.update();
		return w.status;
	}
	
	
	public int vehicleMinAvailability(int V0, double lb, double up) {
		int min_cav = V0;
		int av = min_cav;
		SortedList<BookingRecordWrapper> sublist = b.getCarAvailability(lb, up);
		for(BookingRecordWrapper w : sublist) {
			//if(!w.status) continue;
			if(w.isDemand) { // is pick up (-)
				av -= w.record.getNbrOfVeh();
			} else { // is drop off (+)
				av += w.record.getNbrOfVeh();
			}
			min_cav = (av < min_cav) ? av:min_cav; 
		}
		
		return min_cav;
	}
	
	public int parkingMinAvailability(int V0, double lb, double up) {
		int min_pav = b.station.parking().getCapacity() - V0;
		int pav = min_pav;
		SortedList<BookingRecordWrapper> sublist = b.getParkAvailability(lb, up);
		for(BookingRecordWrapper w : sublist) {
			//if(!w.status) continue;
			if(!w.isDemand) { // is drop off (-)
				pav -= w.record.getNbrOfVeh();
			} else { // is pick up (+)
				pav += w.record.getNbrOfVeh();
			}
			min_pav = (pav < min_pav) ? pav:min_pav; 
		}
		return min_pav;
	}
	
	public CarsharingStationMobsim getStation() {
		return this.b.getStation();
	}
	
	public CarsharingBookingStation getBooking() {
		return this.b;
	}
	
	
	public double getCoef() {
		return coef;
	}
	
	public void setCoeff(double coefficient) {
		coef = coefficient;
	}	
	
}
