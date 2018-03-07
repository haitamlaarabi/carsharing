package org.matsim.contrib.gcs.carsharing.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;

public class CarsharingBookingManager {
	
	private static Logger logger = Logger.getLogger(CarsharingBookingManager.class);

	private final CarsharingOfferModel offermodel;
	private final ConcurrentHashMap<Leg, CarsharingBookingRecord> demandBookingMap;
	private final ConcurrentHashMap<CarsharingStationMobsim, CarsharingBookingStation> stationBookingMap; 
	
	public CarsharingBookingManager(CarsharingManager manager, CarsharingOfferModel omodel) {
		this.offermodel = omodel;
		this.demandBookingMap = new ConcurrentHashMap<Leg, CarsharingBookingRecord>();
		this.stationBookingMap = new ConcurrentHashMap<CarsharingStationMobsim, CarsharingBookingStation>();
	}
	
	public void reset(int iteration) {
		this.demandBookingMap.clear();
		this.stationBookingMap.clear();
	}

	synchronized public boolean process(CarsharingBookingRecord br) {
		
		CarsharingDemand demand = br.getDemand();
		CarsharingStationMobsim So = br.getOriginStation();
		CarsharingStationMobsim Sd = br.getDestinationStation();

		// save booking
		if(demand != null)
			this.demandBookingMap.put(demand.getID(), br);
		
		if(So == null || Sd == null || br.relatedOffer == null) {
			logger.warn("[B-KO] Agent:" + br.getAgent().getId());
			return false;
		}
		
		br.noVehicleOffer = !this.stationBookingMap.get(So).add(br);
		br.noParkingOffer = !this.stationBookingMap.get(Sd).add(br);
		if(br.noVehicleOffer || br.noParkingOffer) {
			this.stationBookingMap.get(So).cancel(br);
			this.stationBookingMap.get(Sd).cancel(br);
			return false;	
		}
		return true;
		
		/*if(So == null || Sd == null) {
			logger.warn("[B-KO] Agent:" + br.getAgent().getId());
			return false;
		}
		
		if(br.relatedOffer == null) return false;
		br.noVehicleOffer = !this.stationBookingMap.get(So).add(br);
		if(br.noVehicleOffer) return false;	
		br.noParkingOffer = !this.stationBookingMap.get(Sd).add(br);
		if(br.noParkingOffer) return false;
		
		return true;*/
	}

	public CarsharingOfferModel offer() {
		return this.offermodel;
	}
	
	public CarsharingBookingRecord getRecord(Leg demandID) {
		return this.demandBookingMap.get(demandID);
	}
	

	public Collection<CarsharingBookingRecord> records() {
		return this.demandBookingMap.values();
	}
	
	public CarsharingBookingStation track(CarsharingStationMobsim station) {
		CarsharingBookingStation booking = this.stationBookingMap.get(station);
		if(booking == null) {
			booking = new CarsharingBookingStation(station);
			this.stationBookingMap.put(station, booking);
		}
		return booking;
	}
	
	

	
}
