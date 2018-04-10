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
			br.vehicleOffer = this.stationBookingMap.get(So).add(br);
			br.parkingOffer = this.stationBookingMap.get(Sd).add(br);
			if(!br.vehicleOffer || !br.parkingOffer) {
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
		CarsharingStationMobsim aStation = null;
		CarsharingStationMobsim eStation = null;
		CarsharingOffer theoffer = null;
		for(CarsharingOffer o : offers) {
			aStation = o.getAccess().getStation();
			eStation = o.getEgress().getStation();
			if(aStation != null && eStation != null) {
				theoffer = o;
				break;
			} else if(aStation != null || eStation != null) {
				theoffer = o;
			}
		}
		
		CarsharingBookingRecord br = null;
		if(aStation != null && eStation != null) {
			br = CarsharingBookingRecord.constructAndGetBookingRec(now, theoffer);
		} else {
			Facility start = CarsharingUtils.getDummyFacility(demand.getOrigin());
			Facility end = CarsharingUtils.getDummyFacility(demand.getDestination());
			Boolean novehiclefound = null;
			Boolean noparkingfound = null;
			double deptime = now;
			if(aStation != null) {
				this.track(aStation);
				start = aStation.facility();
				deptime += theoffer.getAccess().getTravelTime() + this.m.getConfig().getInteractionOffset();
				novehiclefound = !theoffer.getAccess().getStatus().isValid();
			} 
			if(eStation != null){
				this.track(eStation);
				end = eStation.facility();
				noparkingfound = !theoffer.getEgress().getStatus().isValid();
			}
			br = CarsharingBookingRecord.constructBookingRec(
					now, demand, novehiclefound, aStation, deptime,	noparkingfound,	eStation, demand.getRawArrivalTime());
		}
		
		return br;
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
