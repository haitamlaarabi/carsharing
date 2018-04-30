package org.matsim.contrib.gcs.carsharing.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;

public class CarsharingBookingStation {
	
	protected final CarsharingStationMobsim station;
	protected final Map<CarsharingBookingRecord, BookingRecordWrapper> booking_wrapper;
	protected final SortedList<BookingRecordWrapper> car_availability_wrapper;
	protected final SortedList<BookingRecordWrapper> park_availability_wrapper;
	protected int car_availability_tracker;
	protected int park_availability_tracker;
	
	protected class BookingRecordWrapper {
		public CarsharingBookingRecord record;
		public boolean isDemand;
		public double car_availability_time;
		public double park_availability_time;
		public int car_availability_flag;
		public int park_availability_flag;
		public boolean status = false;
		public BookingRecordWrapper(double time, boolean car_availability) {
			if(car_availability) {
				this.car_availability_time = time;
			} else {
				this.park_availability_time = time;
			}
		}
		public BookingRecordWrapper(CarsharingBookingRecord record) {
			this.record = record;
			this.isDemand = record.getOriginStation() != null && record.getOriginStation().equals(station);
			if(!this.isDemand) {
				this.isDemand = !(record.getDestinationStation() != null && record.getDestinationStation().equals(station));
				if(this.isDemand) {
					throw new RuntimeException("Wrong Booking Wrapper !!");
				}
			} 
			CarsharingOffer offer = record.getRelatedOffer();
			if(this.isDemand) {
				this.car_availability_time = record.getDepartureTime();
				this.park_availability_time = (offer == null)? record.getDepartureTime() : offer.getDrive().getTime();
			} else {
				this.car_availability_time = (offer == null)? record.getArrivalTime() : offer.getEgressTime();
				this.park_availability_time = record.getDepartureTime();
			}
			car_availability_wrapper.add(this);
			park_availability_wrapper.add(this);
			booking_wrapper.put(record, this);
		}
		public void update() {
			this.car_availability_flag = car_availability_tracker;
			this.park_availability_flag = car_availability_tracker;
		}
	}
	
	public CarsharingBookingStation(CarsharingStationMobsim s) {
		super();
		this.station = s;
		this.booking_wrapper = new HashMap<CarsharingBookingRecord, BookingRecordWrapper>();
		this.car_availability_wrapper = new SortedList<BookingRecordWrapper>(new Comparator<BookingRecordWrapper>() {
			@Override
			public int compare(BookingRecordWrapper o1, BookingRecordWrapper o2) {
				return Double.compare(o1.car_availability_time, o2.car_availability_time);
			}
		});
		this.park_availability_wrapper = new SortedList<BookingRecordWrapper>(new Comparator<BookingRecordWrapper>() {
			@Override
			public int compare(BookingRecordWrapper o1, BookingRecordWrapper o2) {
				return Double.compare(o1.park_availability_time, o2.park_availability_time);
			}
		});
		if(s.parking() == null) {
			this.car_availability_tracker = s.deployment().size();
			this.park_availability_tracker = s.getCapacity() - s.deployment().size();
		} else {
			this.car_availability_tracker = s.parking().getFleetSize();
			this.park_availability_tracker = s.parking().getCapacity() - s.parking().getFleetSize();
		}
	}
	
	public CarsharingBookingStation(CarsharingBookingStation b) {
		this(CarsharingStationFactory.getStationCopy(b.station));
		this.car_availability_tracker = b.station.deployment().size();
		this.park_availability_tracker = b.station.parking().getCapacity() - b.station.deployment().size();
		for(BookingRecordWrapper w : b.car_availability_wrapper) {
			if(w.isDemand) {
				w.record.setOriginStation(this.station);
			} else {
				w.record.setDestinationStation(this.station);
			}
			this.add(w.record);
		}
	}
		
	public CarsharingStationMobsim getStation() {
		return this.station;
	}
	
	public void confirm(CarsharingBookingRecord record) {
		BookingRecordWrapper w = this.booking_wrapper.get(record);
		if(w.isDemand) {
			// increase parking availability after the vehicle(s) left the station. We don't do this at the booking since the vehicle(s) are still parked
			this.park_availability_tracker += record.getNbrOfVeh(); 
		} else {
			// increase car availability after the vehicle(s) arrives to the station. We don't do this at the booking since the vehicle(s) didn't arrive yet
			this.car_availability_tracker += record.getNbrOfVeh();
		}
	}
	
	public void cancel(CarsharingBookingRecord record) {
		BookingRecordWrapper w = this.booking_wrapper.get(record);
		if(w.status) {
			if(w.isDemand) {
				this.car_availability_tracker += record.getNbrOfVeh();
			} else {
				this.park_availability_tracker += record.getNbrOfVeh();
			}
		}
	}
		
	public boolean add(CarsharingBookingRecord record) {
		BookingRecordWrapper w = new BookingRecordWrapper(record);
		if(w.isDemand && record.getNbrOfVeh() <= this.car_availability_tracker) {
			this.car_availability_tracker -= record.getNbrOfVeh(); // decrease car availability, or in other words to book a vehicle
			w.status = true;
		} else if (!w.isDemand && record.getNbrOfVeh() <= this.park_availability_tracker) {
			this.park_availability_tracker -= record.getNbrOfVeh(); // decrease parking availability, or in other words to book a parking slot
			w.status = true;
		} 
		if(this.car_availability_tracker < 0 || this.park_availability_tracker > this.station.parking().getCapacity()) {
			throw new RuntimeException("Availability in station "+ this.station + " is " + this.car_availability_tracker);
		}
		w.update();
		return w.status;
	}
	
	// *********
	
	protected SortedList<BookingRecordWrapper> getCarAvailability(double lb, double ub) {
		BookingRecordWrapper lb_w = new BookingRecordWrapper(lb, true);
		BookingRecordWrapper ub_w = new BookingRecordWrapper(ub, true);
		SortedList<BookingRecordWrapper> sublist = this.car_availability_wrapper.subList(lb_w, ub_w);
		return sublist;
	}
	
	protected SortedList<BookingRecordWrapper> getParkAvailability(double lb, double ub) {
		BookingRecordWrapper lb_w = new BookingRecordWrapper(lb, false);
		BookingRecordWrapper ub_w = new BookingRecordWrapper(ub, false);
		SortedList<BookingRecordWrapper> sublist = this.park_availability_wrapper.subList(lb_w, ub_w);
		return sublist;
	} 
	
	// *********
	
	public CarsharingBookingRecord[] getDemand(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : getCarAvailability(lowerBorn, upperBorn)) {
			if(w.isDemand) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	public CarsharingBookingRecord[] getSupply(double lowerBorn, double upperBorn) {
		ArrayList<CarsharingBookingRecord> records = new ArrayList<CarsharingBookingRecord>();
		for(BookingRecordWrapper w : getParkAvailability(lowerBorn, upperBorn)) {
			if(!w.isDemand) {
				records.add(w.record);
			}
		}
		return records.toArray(new CarsharingBookingRecord[0]);
	}
	
	// *********
	
	public CarsharingBookingRecord[] getDemand(double time) {
		return this.getDemand(0.0, time);
	}
	
	public CarsharingBookingRecord[] getSupply(double time) {
		return this.getSupply(0.0, time);
	}

	// *********
		
	public int vehicleAvailability() {
		return this.car_availability_tracker;
	}
	
	public int vehicleAvailability(double time) {		
		SortedList<BookingRecordWrapper> sublist = getCarAvailability(0, time);
		if(sublist.isEmpty()) 
			return this.car_availability_tracker;
		else 
			return sublist.getLast().car_availability_flag;
	}
	
	// *********
	
	public int parkingAvailability() {
		return this.park_availability_tracker;
	}
	
	public int parkingAvailability(double time) {
		SortedList<BookingRecordWrapper> sublist = getParkAvailability(0, time);
		if(sublist.isEmpty()) 
			return this.park_availability_tracker;
		else 
			return sublist.getLast().park_availability_flag;
	}
	
}
