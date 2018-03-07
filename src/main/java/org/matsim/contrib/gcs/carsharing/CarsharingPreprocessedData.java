package org.matsim.contrib.gcs.carsharing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;
import org.matsim.facilities.ActivityFacility;

public class CarsharingPreprocessedData {
	
	class CarsharingPreprocessedIteration {
		HashSet<CarsharingBookingRecord> bookingSuccess;
		HashSet<CarsharingBookingRecord> bookingFailure;
		final ConcurrentHashMap<Id<ActivityFacility>, CarsharingBookingStation> stationsWrapperMap; 
		final ConcurrentHashMap<CarsharingOperatorMobsim, ArrayList<CarsharingRelocationTask>> relocationWrapperMap;
		public CarsharingPreprocessedIteration() {
			this.stationsWrapperMap = new ConcurrentHashMap<Id<ActivityFacility>, CarsharingBookingStation>();
			this.relocationWrapperMap = new ConcurrentHashMap<CarsharingOperatorMobsim, ArrayList<CarsharingRelocationTask>>();
			this.bookingSuccess = new HashSet<CarsharingBookingRecord>();
			this.bookingFailure = new HashSet<CarsharingBookingRecord>();
		}
	}
	
	ConcurrentHashMap<Integer, CarsharingPreprocessedIteration> data = new ConcurrentHashMap<Integer, CarsharingPreprocessedIteration>();
	Integer lastIteration = -1;
	
	

	
	public ConcurrentHashMap<Id<ActivityFacility>, CarsharingBookingStation> stationMap(int iteration) {
		return data.get(new Integer(iteration)).stationsWrapperMap;
	}
	
	public ConcurrentHashMap<CarsharingOperatorMobsim, ArrayList<CarsharingRelocationTask>>  relocationMap(int iteration) {
		return data.get(new Integer(iteration)).relocationWrapperMap;
	}
	
	
	
	public HashSet<CarsharingBookingRecord> bookingSuccessSet() {
		if(lastIteration >= 0)
			return data.get(new Integer(lastIteration)).bookingSuccess; 
		return new HashSet<CarsharingBookingRecord>();
	}
	
	public HashSet<CarsharingBookingRecord> bookingFailureSet() {
		if(lastIteration >= 0)
			return data.get(new Integer(lastIteration)).bookingFailure; 
		return new HashSet<CarsharingBookingRecord>();
	}
	
	public ConcurrentHashMap<Id<ActivityFacility>, CarsharingBookingStation> stationMap() {
		if(lastIteration >= 0)
			return stationMap(lastIteration);
		return new ConcurrentHashMap<Id<ActivityFacility>, CarsharingBookingStation>();
	}
	
	public ConcurrentHashMap<CarsharingOperatorMobsim, ArrayList<CarsharingRelocationTask>> relocationMap() {
		if(lastIteration >= 0)
			return relocationMap(lastIteration);
		return new ConcurrentHashMap<CarsharingOperatorMobsim, ArrayList<CarsharingRelocationTask>>();
	}
	
	/**
	 * 
	 * @param iteration
	 * @param manager
	 */
	public void update(Integer iteration, CarsharingManager manager) {
		lastIteration = iteration;
		CarsharingPreprocessedIteration it = new CarsharingPreprocessedIteration();
		for(CarsharingStationMobsim station : manager.getStations()) {
			it.stationsWrapperMap.put(
					station.facility().getId(), 
					new CarsharingBookingStation(manager.booking().track(station)));			
		}
		for(CarsharingBookingRecord dm : manager.booking().records()) {
			if(dm.bookingFailed() || dm.trip() == null || dm.park() == null) {
				it.bookingFailure.add(dm);
			} else {
				it.bookingSuccess.add(dm);
			}
		}
		for(CarsharingOperatorMobsim op : manager.getOperators()) {
			it.relocationWrapperMap.put(op, op.getAllTasks());
		}
		this.data.put(iteration, it);
	}
	
	public boolean isEmpty() {
		return this.data.isEmpty();
	}
	
}
