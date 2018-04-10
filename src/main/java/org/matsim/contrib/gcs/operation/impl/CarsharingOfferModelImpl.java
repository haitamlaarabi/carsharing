package org.matsim.contrib.gcs.operation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer.CarsharingOfferStatus;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule.CarsharingLocationInfo;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class CarsharingOfferModelImpl implements CarsharingOfferModel  {


	// Attribute
	Scenario scenario;
	CarsharingManager manager;
	CarsharingConfigGroup cs_conf;
	
	Network network;
	boolean floatingStations;
	double tau = 2.0 * 3600.0;
	double alpha = 0.7;
	private double timeFeePerMinute; // 1 euro per minute
	private int time;

	private TripRouter router;
	final CarsharingNearestStationRouterModule nearStationRouter;
	
	@Inject
	CarsharingOfferModelImpl(Scenario scenario, CarsharingManager manager, Provider<TripRouter> tripRouterProvider) {
		this.scenario = scenario;
		this.manager = manager;
		this.cs_conf = manager.getConfig();
		this.network = this.scenario.getNetwork();
		this.timeFeePerMinute = manager.getConfig().getDriveCalcScore().getMonetaryDistanceRate();
		this.floatingStations = false;
		this.router = tripRouterProvider.get();
		this.nearStationRouter = new CarsharingNearestStationRouterModule(this.scenario, this.manager, null);
	}

	
	@Override
	public double computeRentalCost(CarsharingOffer offer, double rentalDuration) {
		double total = rentalDuration * offer.getCost();
		return total;
	}
	
	@Override
	public ArrayList<CarsharingOffer> computeRentalOffers(int time, CarsharingDemand demand) {
		this.time = time;
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		for(CarsharingOffer depOffer : calculateDepartureOffers(demand)) {
			for(CarsharingOffer finalOffer : calculateArrivalOffers(depOffer)) {
				offers.add(finalOffer);
			}
		}
		return offers;
		/*for(CarsharingOffer depOffer : calculateDepartureOffers(demand)) {
			if(depOffer.hasValidAccess()) {
				for(CarsharingOffer arrOffer : calculateArrivalOffers(depOffer)) {
					if(arrOffer.hasValidEgress()) {
						offers.add(arrOffer);
					} else {
						failedoffers.add(arrOffer);
					}
				}
			} else {
				failedoffers.add(depOffer);
			}
		}
		if(offers.isEmpty()) {
			if(better_walk) {
				offers.add(getWalkOffer(demand, CarsharingOffer.FAILURE_WALK_OFFER));
			} else {
				offers.addAll(failedoffers);
			}
		}*/
	}
	
	// ***********************************************************************************
	// ***********************************************************************************
	
	/**
	 * 
	 * @param demand
	 * @return
	 */
	public ArrayList<CarsharingOffer> calculateDepartureOffers(CarsharingDemand demand) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		CarsharingLocationInfo closest_station = this.nearStationRouter.getNearestStationToDeparture(demand.getOrigin().getCoord());
		
		if(closest_station.station == null) {
			offers.add(this.getAccessStationOffer(demand, closest_station, CarsharingOffer.FAILURE_NODEPARTURESTATION));
		} else if(manager.booking().track(closest_station.station).vehicleAvailability() < demand.getNbrOfVeh()) {
			offers.add(this.getAccessStationOffer(demand, closest_station, CarsharingOffer.FAILURE_NODEPARTUREAVAILABILITY));
		} else {
			offers.add(this.getAccessStationOffer(demand, closest_station, CarsharingOffer.SUCCESS_STANDARDOFFER));
		}

		return offers;
	}

	
	/**
	 * calculateArrivalOffers
	 */
	public ArrayList<CarsharingOffer> calculateArrivalOffers(CarsharingOffer offer) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		CarsharingLocationInfo closest_station = this.nearStationRouter.getNearestStationToArrival(offer.getDemand().getDestination().getCoord(), offer.getAccess().getStation());
		
		if(closest_station.station == null) {
			offers.add(this.getEgressStationOffer(offer, closest_station, CarsharingOffer.FAILURE_NOARRIVALSTATION));
		} else if(manager.booking().track(closest_station.station).parkingAvailability() < offer.getDemand().getNbrOfVeh()) {
			offers.add(this.getEgressStationOffer(offer, closest_station, CarsharingOffer.FAILURE_NOARRIVALAVAILABILITY));
		} else {
			offers.add(this.getEgressStationOffer(offer, closest_station, CarsharingOffer.SUCCESS_STANDARDOFFER));
		}
		
		// FLOATING OFFER
		if(this.floatingStations) 
			offers.add(this.getEgressStationFloatingOffer(offer));
		// USER RELOCATION TIME
		offers.addAll(manager.relocation().relocationList(time, offer.getDemand(), offers));
		return offers;
	}
	
	/**
	 * 
	 * @param demand
	 * @param selectedStation
	 * @param monetaryOption
	 * @param roadTrainSize
	 */
	public CarsharingOffer getAccessStationOffer(CarsharingDemand demand, CarsharingLocationInfo s, CarsharingOfferStatus flag) {

		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromAgent(demand.getAgent(), demand);
		builder.setAccess(this.time, s.station, s.traveltime, s.distance, flag);
		builder.setCost(this.timeFeePerMinute);
		builder.setDrive(demand.getNbrOfVeh());
		return builder.build();
	}
	
	/**
	 * 
	 * @param o
	 * @param s
	 * @return
	 */
	public CarsharingOffer getEgressStationOffer(CarsharingOffer o, CarsharingLocationInfo s, CarsharingOfferStatus flag) {
		
		RouteData rd = null;
		if(flag != CarsharingOffer.FAILURE_NOARRIVALSTATION && o.getAccess().getStatus().isValid()) {
			rd = CarsharingRouterUtils.calcTCC(manager, 
					o.getAccess().getStation().facility(), 
					s.station.facility(), 
					o.getAccessTime(), 
					o.getDemand().getAgent().getPerson());
		}
				
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(o);
		builder.setDrive(o.getNbOfVehicles(), rd);
		builder.setEgress(o.getDemand().getRawArrivalTime(), s.station, s.traveltime, s.distance, flag);
		return builder.build();
	}
	

	/**
	 * 
	 * @param offer
	 * @return
	 */
	public CarsharingOffer getEgressStationFloatingOffer(CarsharingOffer offer) {
		
			// FLOATING OFFERS
			int min = manager.getStations().qtree().size();
			int max = min * 1000;
			int sid = MatsimRandom.getRandom().nextInt(max - min + 1) + min;
			int sid2 = MatsimRandom.getRandom().nextInt(max - min + 1) + min;
			
			String newFSid = sid2 + sid + "";
	
			CarsharingStation newFS = CarsharingStationFactory.stationBuilder(
					scenario, newFSid, 
					offer.getDemand().getDestination().getCoord()).
						setCapacity(offer.getNbOfVehicles()).
						setType("FLOATING").
						build();
			
			// GET DURATION DISTANCE STATION ACTIVITY
			
			final double egressDist = NetworkUtils.getEuclideanDistance(newFS.facility().getCoord(), offer.getDemand().getDestination().getCoord());
			final double traveltime = CarsharingUtils.travelTimeBeeline(egressDist, this.cs_conf.getEgressWalkCalcRoute());
			final double distance = CarsharingUtils.distanceBeeline(egressDist, this.cs_conf.getEgressWalkCalcRoute());
			
			RouteData rd = CarsharingRouterUtils.calcTCC(manager, 
					offer.getAccess().getStation().facility(), 
					newFS.facility(), 
					offer.getAccessTime(), 
					offer.getDemand().getAgent().getPerson());
			
			CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(offer);
			builder.setDrive(offer.getNbOfVehicles(), rd);
			builder.setEgress((CarsharingStationMobsim) newFS, traveltime, distance, CarsharingOffer.SUCCESS_FREEFLOATINGOFFER);
			return builder.build();
	}
	
}
