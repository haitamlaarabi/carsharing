package org.matsim.contrib.gcs.carsharing.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;

public class CarsharingBookingStation {
	
	protected final CarsharingStationMobsim station;
	protected final Map<CarsharingBookingRecord, BookingRecordWrapper> booking_wrapper;
	protected final SortedList<BookingRecordWrapper> activity;
	protected int car_availability_tracker;
	protected int parking_availability_tracker;
	
	protected class BookingRecordWrapper {
		public CarsharingBookingRecord record;
		public boolean isDemand;
		public double time;
		public int parking_availability_flag;
		public int car_availability_flag;
		public BookingRecordWrapper(double t) {
			this.time = t;
		}
		public BookingRecordWrapper(CarsharingBookingRecord record) {
			if(record.getOriginStation() != null && record.getOriginStation().equals(station)) {
				this.isDemand = true;
				this.time = record.getRelatedOffer().getDrive().getTime();
			} else if(record.getDestinationStation() != null && record.getDestinationStation().equals(station)) {
				this.isDemand = false;
				this.time = record.getRelatedOffer().getEgress().getTime() - record.getRelatedOffer().getDrive().getOffset();
			} else {
				throw new RuntimeException("Wrong Booking Wrapper !!");
			}
			this.record = record;
		}
		public double getTime() {
			return this.time;
		}
	}
	
	public CarsharingBookingStation(CarsharingBookingStation b) {
		this(CarsharingStationFactory.getStationCopy(b.station));
		this.car_availability_tracker = b.station.initialFleet().size();
		this.parking_availability_tracker = b.station.parking().getCapacity() - b.station.initialFleet().size();
		for(BookingRecordWrapper w : b.activity) {
			if(w.isDemand) {
				w.record.setOriginStation(this.station);
			} else {
				w.record.setDestinationStation(this.station);
			}
			this.add(w.record);
		}
	}
	
	public CarsharingBookingStation(CarsharingStationMobsim s) {
		this.station = s;
		this.activity = new SortedList<BookingRecordWrapper>(new Comparator<BookingRecordWrapper>() {
			@Override
			public int compare(BookingRecordWrapper o1, BookingRecordWrapper o2) {
				return Double.compare(o1.getTime(), o2.getTime());
			}
		});
		this.car_availability_tracker = s.parking().getFleetSize();
		this.parking_availability_tracker = s.parking().getCapacity() - s.parking().getFleetSize();
		this.booking_wrapper = new HashMap<CarsharingBookingRecord, BookingRecordWrapper>();
	}
	
	public CarsharingStationMobsim getStation() {
		return this.station;
	}
	
	public void confirm(CarsharingBookingRecord record) {
		if(this.booking_wrapper.get(record).isDemand) {
			// increase parking availability after the vehicle(s) left the station. We don't do this at the booking since the vehicle(s) are still parked
			this.parking_availability_tracker += record.getNbrOfVeh(); 
		} else {
			// increase car availability after the vehicle(s) arrives to the station. We don't do this at the booking since the vehicle(s) didn't arrive yet
			this.car_availability_tracker += record.getNbrOfVeh();
		}
	}
	
	public boolean add(CarsharingBookingRecord record) {
		BookingRecordWrapper w = new BookingRecordWrapper(record);
		boolean booking = true;
		if(w.isDemand && record.getNbrOfVeh() <= this.car_availability_tracker) {
			// decrease car availability, or in other words to book a vehicle
			this.car_availability_tracker -= record.getNbrOfVeh(); 
		} else if (!w.isDemand && record.getNbrOfVeh() <= this.parking_availability_tracker) {
			// decrease parking availability, or in other words to book a parking slot
			this.parking_availability_tracker -= record.getNbrOfVeh(); // INCREASE VEHICLE UPPER BOUND
		} else {
			booking = false;
		}
		if(this.car_availability_tracker < 0 || this.parking_availability_tracker > this.station.parking().getCapacity()) {
			throw new RuntimeException("Availability in station "+ this.station + " is " + this.car_availability_tracker);
		}
		w.car_availability_flag = this.car_availability_tracker;
		w.parking_availability_flag = this.parking_availability_tracker;
		this.activity.add(w);
		this.booking_wrapper.put(record, w);
		return booking;
	}
	
	public CarsharingBookingRecord[] getDemand(double time) {
		return this.getDemand(0.0, time);
	}
	
	public CarsharingBookingRecord[] getSupply(double time) {
		return this.getSupply(0.0, time);
	}
	
	public CarsharingBookingRecord[] getDemand(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lowerBorn), new BookingRecordWrapper(upperBorn))) {
			if(w.isDemand) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	public CarsharingBookingRecord[] getSupply(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lowerBorn), new BookingRecordWrapper(upperBorn))) {
			if(!w.isDemand) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	public CarsharingBookingRecord[] getSatisfiedDemand(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lowerBorn), new BookingRecordWrapper(upperBorn))) {
			if(w.isDemand && !w.record.bookingFailed()) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);

	}
	
	
	public CarsharingBookingRecord[] getSatisfiedSupply(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lowerBorn), new BookingRecordWrapper(upperBorn))) {
			if(!w.isDemand && !w.record.bookingFailed()) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	
	public CarsharingBookingRecord[] getOperatorSupply(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lowerBorn), new BookingRecordWrapper(upperBorn))) {
			if(!w.isDemand && !w.record.bookingFailed() && w.record.getAgent() instanceof CarsharingOperatorMobsim) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	public int vehicleAvailability() {
		return this.car_availability_tracker;
	}
	public int vehicleAvailability(double time) {
		SortedList<BookingRecordWrapper> sub = this.activity.subList(new BookingRecordWrapper(0), new BookingRecordWrapper(time));
		if(sub.isEmpty()) 
			return this.car_availability_tracker;
		else 
			return sub.getLast().car_availability_flag;
	}
	
	public int parkingAvailability() {
		return this.parking_availability_tracker;
	}
	public int parkingAvailability(double time) {
		SortedList<BookingRecordWrapper> sub = this.activity.subList(new BookingRecordWrapper(0), new BookingRecordWrapper(time));
		if(sub.isEmpty()) 
			return this.parking_availability_tracker;
		else 
			return sub.getLast().parking_availability_flag;
	}
	
	
	public int vehicleMinAvailability(double lb, double up) {
		int min_cav = Integer.MAX_VALUE;
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lb), new BookingRecordWrapper(up))) {
			if(min_cav > w.car_availability_flag)
				min_cav = w.car_availability_flag;
		}
		if(min_cav == Integer.MAX_VALUE) 
			min_cav = vehicleAvailability(lb);
		return min_cav;
	}
	
	
	public int parkingMinAvailability(double lb, double up) {
		int min_pav = Integer.MAX_VALUE;
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lb), new BookingRecordWrapper(up))) {
			if(min_pav > w.parking_availability_flag)
				min_pav = w.parking_availability_flag;
		}
		if(min_pav == Integer.MAX_VALUE) 
			min_pav = parkingAvailability(lb); 
		return min_pav;
	}
	
}
