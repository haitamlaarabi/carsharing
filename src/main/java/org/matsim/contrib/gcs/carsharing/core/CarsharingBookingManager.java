package org.matsim.contrib.gcs.carsharing.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule.CarsharingLocationInfo;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.facilities.Facility;

public class CarsharingBookingManager {
	
	private static Logger logger = Logger.getLogger(CarsharingBookingManager.class);

	private final CarsharingOfferModel offermodel;
	private final ConcurrentHashMap<Leg, CarsharingBookingRecord> demandBookingMap;
	private final ConcurrentHashMap<CarsharingStationMobsim, CarsharingBookingStation> stationBookingMap; 
	final CarsharingManager m;
	final CarsharingNearestStationRouterModule nearStationRouter;
	
	public CarsharingBookingManager(CarsharingManager manager, CarsharingOfferModel omodel) {
		this.offermodel = omodel;
		this.demandBookingMap = new ConcurrentHashMap<Leg, CarsharingBookingRecord>();
		this.stationBookingMap = new ConcurrentHashMap<CarsharingStationMobsim, CarsharingBookingStation>();
		this.m = manager;
		this.nearStationRouter = new CarsharingNearestStationRouterModule(this.m.getScenario(), this.m, null);
	}
	
	public void reset(int iteration) {
		this.demandBookingMap.clear();
		this.stationBookingMap.clear();
	}

	synchronized public CarsharingBookingRecord process(double now, CarsharingDemand demand, CarsharingOffer selectedOffer, ArrayList<CarsharingOffer> listOfOffers) {
		
		CarsharingBookingRecord br = null;
		if(selectedOffer != null) {
			br = CarsharingBookingRecord.constructAndGetBookingRec(now, selectedOffer);
			CarsharingStationMobsim So = br.getOriginStation();
			CarsharingStationMobsim Sd = br.getDestinationStation();
			br.noVehicleOffer = !this.stationBookingMap.get(So).add(br);
			br.noParkingOffer = !this.stationBookingMap.get(Sd).add(br);
			if(br.noVehicleOffer || br.noParkingOffer) {
				this.stationBookingMap.get(So).cancel(br);
				this.stationBookingMap.get(Sd).cancel(br);
			}
		} else {
			br = constructFailedRecord(now, listOfOffers, demand);
			CarsharingStationMobsim So = br.getOriginStation();
			CarsharingStationMobsim Sd = br.getDestinationStation();
			if(So != null) {
				// keep in memory
				this.stationBookingMap.get(So).add(br);
				this.stationBookingMap.get(So).cancel(br);
			}
			if(Sd != null)  {
				// keep in memory
				this.stationBookingMap.get(Sd).add(br);
				this.stationBookingMap.get(Sd).cancel(br);
			}
		}

		// save booking
		if(demand != null)
			this.demandBookingMap.put(demand.getID(), br);
		
		return br;
	}
	
	
	private CarsharingBookingRecord constructFailedRecord(double now, ArrayList<CarsharingOffer> offers, CarsharingDemand demand) {
		CarsharingLocationInfo departure = this.nearStationRouter.getNearestStationToDeparture(CarsharingUtils.getDummyFacility(demand.getOrigin()));
		CarsharingLocationInfo arrival = this.nearStationRouter.getNearestStationToArrival(CarsharingUtils.getDummyFacility(demand.getDestination()), departure.station);
		Facility start = departure.facility;
		Facility end = arrival.facility;
		double deptime = now;
		if(departure.station != null) {
			this.track(departure.station);
			start = departure.station.facility();
			deptime += departure.traveltime + this.m.getConfig().getInteractionOffset();
		}
		if(arrival.station != null){
			this.track(arrival.station);
			end = arrival.station.facility();
		}
		Boolean novehiclefound = true;
		Boolean noparkingfound = true;
		boolean betterWalk = false;
		for(CarsharingOffer o : offers) {
			if(!o.hasValidAccess() && o.getAccess().getStatus().equals(CarsharingOffer.FAILURE_WALK_OFFER)) {
				betterWalk = true;
				break;
			} else if(o.hasValidAccess()) { 
				novehiclefound = false; 
				break;	
			} 
		}
		CarsharingBookingRecord booking = CarsharingBookingRecord.constructAndGetFailedBookingRec(
				now, demand, betterWalk,
				novehiclefound,	departure.station, deptime,
				noparkingfound,	arrival.station, Double.NaN);
		return booking;
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
