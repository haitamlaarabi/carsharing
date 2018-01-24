package org.matsim.contrib.gcs.carsharing.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;

public class CarsharingBookingStation {
	
	private static Logger logger = Logger.getLogger(CarsharingBookingStation.class);

	
	protected final CarsharingStationMobsim station;
	protected final Map<CarsharingBookingRecord, BookingRecordWrapper> booking_wrapper;
	protected final SortedList<BookingRecordWrapper> activity;
	protected int lower_bound_availability_tracker;
	protected int upper_bound_availability_tracker;
	
	
	protected class BookingRecordWrapper {
		public CarsharingBookingRecord record;
		public boolean isDemand;
		public double time;
		public int upper_bound_flag;
		public int lower_bound_flag;
		public BookingRecordWrapper(double t) {
			this.time = t;
		}
		public BookingRecordWrapper(CarsharingBookingRecord record) {
			if(record.getOriginStation() != null && record.getOriginStation().equals(station)) {
				this.isDemand = true;
				//this.time = record.departureTime + record.getRelatedOffer().getAccess().getTravelTime();
				this.time = record.getRelatedOffer().getDrive().getTime();
			} else if(record.getDestinationStation() != null && record.getDestinationStation().equals(station)) {
				this.isDemand = false;
				//this.time = record.arrivalTime - record.getRelatedOffer().getEgress().getTravelTime();
				this.time = record.getRelatedOffer().getEgress().getTime();
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
		this.lower_bound_availability_tracker = b.station.initialFleet().size();
		this.upper_bound_availability_tracker = this.lower_bound_availability_tracker;
		for(BookingRecordWrapper w : this.activity) {
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
		this.lower_bound_availability_tracker = s.parking().getFleetSize();
		this.upper_bound_availability_tracker = this.lower_bound_availability_tracker;
		this.booking_wrapper = new HashMap<CarsharingBookingRecord, BookingRecordWrapper>();
	}
	
	public CarsharingStationMobsim getStation() {
		return this.station;
	}
	
	public void confirm(CarsharingBookingRecord record, CarsharingVehicleMobsim v) {
		if(!this.booking_wrapper.get(record).isDemand) {
			this.lower_bound_availability_tracker += record.getNbrOfVeh(); // INCREASE UPPER BOUND
			record.park = v.status().getPark().getId();
		} else {
			this.upper_bound_availability_tracker -= record.getNbrOfVeh(); // DECREASE UPPER BOUND
			record.setVehicle(v);
			record.trip = v.status().getTrip().getId();
		}
	}
	
	public boolean add(CarsharingBookingRecord record) {
		BookingRecordWrapper w = new BookingRecordWrapper(record);
		boolean flag = true;
		if(w.isDemand && record.getNbrOfVeh() <= this.lower_bound_availability_tracker) {
			this.lower_bound_availability_tracker -= record.getNbrOfVeh(); // DECREASE VEHICLE LOWER BOUND
		} else if (!w.isDemand && record.getNbrOfVeh() <= this.station.parking().getCapacity() - this.upper_bound_availability_tracker) {
			this.upper_bound_availability_tracker += record.getNbrOfVeh(); // INCREASE VEHICLE UPPER BOUND
		} else {
			flag = false;
		}
		if(this.lower_bound_availability_tracker < 0 || this.upper_bound_availability_tracker > this.station.parking().getCapacity()) {
			logger.warn("AVAILABILITY IN STATION " + this.station + " IS GOING OUT OF BOUND " + this.lower_bound_availability_tracker);
			throw new RuntimeException("AVAILABILITY !!");
		}
		w.lower_bound_flag = this.lower_bound_availability_tracker;
		w.upper_bound_flag = this.upper_bound_availability_tracker;
		this.activity.add(w);
		this.booking_wrapper.put(record, w);
		return flag;
		
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
		return this.lower_bound_availability_tracker;
	}
	public int vehicleAvailability(double time) {
		SortedList<BookingRecordWrapper> sub = this.activity.subList(new BookingRecordWrapper(0), new BookingRecordWrapper(time));
		if(sub.isEmpty()) return this.lower_bound_availability_tracker;
		else return sub.getLast().lower_bound_flag;
	}
	
	public int parkingAvailability() {
		return this.station.parking().getCapacity() - this.upper_bound_availability_tracker;
	}
	public int parkingAvailability(double time) {
		SortedList<BookingRecordWrapper> sub = this.activity.subList(new BookingRecordWrapper(0), new BookingRecordWrapper(time));
		if(sub.isEmpty()) return this.station.parking().getCapacity() - this.upper_bound_availability_tracker;
		else return this.station.parking().getCapacity() - sub.getLast().upper_bound_flag;
	}
	
	
	public int vehicleMinAvailability(double lb, double up) {
		int min_av = Integer.MAX_VALUE;
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lb), new BookingRecordWrapper(up))) {
			if(min_av > w.lower_bound_flag)
				min_av = w.lower_bound_flag;
		}
		if(min_av == Integer.MAX_VALUE) min_av = vehicleAvailability(lb);
		return min_av;
	}
	
	
	public int parkingMinAvailability(double lb, double up) {
		int max_av = Integer.MIN_VALUE;
		for(BookingRecordWrapper w : this.activity.subList(new BookingRecordWrapper(lb), new BookingRecordWrapper(up))) {
			if(max_av < w.upper_bound_flag)
				max_av = w.upper_bound_flag;
		}
		int pk_av;
		if(max_av == Integer.MIN_VALUE) 
			pk_av = parkingAvailability(lb); 
		else 
			pk_av = this.station.parking().getCapacity() - max_av;
		
		return pk_av;
	}
	
}
