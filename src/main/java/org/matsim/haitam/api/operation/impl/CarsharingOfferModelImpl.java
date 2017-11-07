package org.matsim.haitam.api.operation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.core.CarsharingBookingStation;
import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.carsharing.core.CarsharingStation;
import org.matsim.haitam.api.carsharing.core.CarsharingStationMobsim;
import org.matsim.haitam.api.carsharing.impl.CarsharingStationFactory;
import org.matsim.haitam.api.operation.model.CarsharingOfferModel;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class CarsharingOfferModelImpl implements CarsharingOfferModel  {

	// Attribute
	Scenario scenario;
	CarsharingManager manager;
	
	Network network;
	double beelineWalkSpeed;
	double beelineDistanceFactor;
	boolean floatingStations;
	double tau = 2.0 * 3600.0;
	double alpha = 0.7;
	private double timeFeePerMinute; // 1 euro per minute
	private double time;
	
	protected static String STANDARD_STATION = "STANDARD";
	protected static String FLOATING_STATION = "FLOATING";
	
	private TripRouter router;
	
	
	@Inject
	CarsharingOfferModelImpl(Scenario scenario, CarsharingManager manager, Provider<TripRouter> tripRouterProvider) {
		this.scenario = scenario;
		this.manager = manager;
		this.network = this.scenario.getNetwork();
		this.beelineWalkSpeed = manager.getConfig().getAccessWalkCalcRoute().getTeleportedModeSpeed();
		this.beelineDistanceFactor = manager.getConfig().getAccessWalkCalcRoute().getBeelineDistanceFactor();
		this.timeFeePerMinute = manager.getConfig().getDriveCalcScore().getMonetaryDistanceRate();
		this.floatingStations = false;
		this.router = tripRouterProvider.get();
	}

	
	@Override
	public double computeRentalCost(CarsharingOffer offer, double rentalDuration) {
		double total = rentalDuration * offer.getCost();
		return total;
	}
	
	@Override
	public ArrayList<CarsharingOffer> computeRentalOffers(double time, CarsharingDemand demand) {
		this.time = time;
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		ArrayList<CarsharingOffer> failedoffers = new ArrayList<CarsharingOffer>();
		Iterator<CarsharingOffer> initialOffersItr = calculateDepartureOffers(demand).iterator();
		while(initialOffersItr.hasNext()) {
			CarsharingOffer initialOffer = initialOffersItr.next();
			if(initialOffer.hasValidAccess()) {
				ArrayList<CarsharingOffer> temp = calculateArrivalOffers(initialOffer);
				offers.addAll(temp);
			} else {
				failedoffers.add(initialOffer);
			}
		}
		if(offers.isEmpty()) {
			offers.addAll(failedoffers);
		}
		return offers;
	}
	
	// ***********************************************************************************
	// ***********************************************************************************
	
	public ArrayList<CarsharingOffer> calculateDepartureOffers(CarsharingDemand demand) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		Map<String, SelectedStation> closestStation = this.getClosestStationToDepartureLink(demand);
		if (closestStation == null || closestStation.isEmpty()) { 
			offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_NODEPARTURESTATION));
		} else {
			SelectedStation floating_station = closestStation.get(FLOATING_STATION);
			SelectedStation standard_station = closestStation.get(STANDARD_STATION);
			// FLOATING OFFERS
			if(floating_station != null && floating_station.station != null) {
					offers.add(this.getAccessStationStandardOffer(demand, floating_station, 0, floating_station.station.parking().getFleetSize()));
			} else {
				offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_FLOATINGLIMIT));
			}
			// STANDARD OFFERS
			if(standard_station != null && standard_station.station != null) {
				offers.add(this.getAccessStationStandardOffer(demand, standard_station, 0, 1));
			} else {
				offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_NODEPARTUREAVAILABILITY));
			}
		}
		return offers;
	}

	
	/**
	 * calculateArrivalOffers
	 */
	public ArrayList<CarsharingOffer> calculateArrivalOffers(CarsharingOffer offer) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		Map<String, SelectedStation> closestStation = this.getClosestStationToArrivalLink(offer.getDemand(), offer.getAccess().getStation());
		if (closestStation == null || closestStation.isEmpty()) { 
			offers.add(getEgressFailureOffer(offer.getDemand(), CarsharingOffer.FAILURE_NOARRIVALSTATION));
		} else {
			SelectedStation standard_station = closestStation.get(STANDARD_STATION);
			if(standard_station != null && standard_station.station != null) {
				offers.add(this.getEgressStationStandardOffer(offer, standard_station));
			} else  {
				offers.add(this.getEgressFailureOffer(offer.getDemand(), CarsharingOffer.FAILURE_NOARRIVALAVAILABILITY));
			}
		}
		// FLOATING OFFER
		if(this.floatingStations) offers.add(this.getEgressStationFloatingOffer(offer));
		// USER RELOCATION TIME
		offers.addAll(manager.relocation().computeUserRelocation(time, offer.getDemand(), offers));
		return offers;
	}
	

	public CarsharingOffer getAccessFailureOffer(CarsharingDemand demand, String failureMsg) {
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromDemand(demand, failureMsg);
		builder.setAccess(failureMsg);
		return builder.build();
	}
	
	public CarsharingOffer getEgressFailureOffer(CarsharingDemand demand, String failureMsg) {
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromDemand(demand, failureMsg);
		builder.setEgress(failureMsg);
		return builder.build();
	}
	
	
	/**
	 * 
	 * @param demand
	 * @param selectedStation
	 * @param monetaryOption
	 * @param roadTrainSize
	 */
	public CarsharingOffer getAccessStationStandardOffer(
			CarsharingDemand demand, 
			SelectedStation selectedStation,
			double monetaryOption,
			int roadTrainSize) {

		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromDemand(demand, CarsharingOffer.SUCCESS_STANDARDOFFER);
		builder.setAccess(this.time, selectedStation.station, selectedStation.traveltime, selectedStation.distance, this.manager.getConfig().getInteractionOffset());
		builder.setCost(this.timeFeePerMinute);
		builder.setDrive(roadTrainSize);
		return builder.build();
	}
	
	/**
	 * 
	 * @param o
	 * @param s
	 * @return
	 */
	public CarsharingOffer getEgressStationStandardOffer(CarsharingOffer o, SelectedStation s) {
		
		List<? extends PlanElement> elements = this.router.calcRoute(
				TransportMode.car, 
				o.getAccess().getStation().facility(), 
				s.station.facility(), 
				o.getDepartureTime() + o.getAccess().getTravelTime() + o.getAccess().getOffsetDur(), 
				o.getDemand().getAgent().getPerson());
		
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(o, CarsharingOffer.SUCCESS_STANDARDOFFER);
		builder.setDrive(o.getNbOfVehicles(), elements);
		builder.setEgress(s.station, s.traveltime, s.distance, this.manager.getConfig().getInteractionOffset());
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
						setType(FLOATING_STATION).
						build();
			
			// GET DURATION DISTANCE STATION ACTIVITY
			
			final double egressDist = NetworkUtils.getEuclideanDistance(
					newFS.facility().getCoord(), 
					offer.getDemand().getDestination().getCoord());
			final double duration = egressDist * this.beelineWalkSpeed;
			final double distance = egressDist * this.beelineDistanceFactor;	
			
			List<? extends PlanElement> elements = this.router.calcRoute(
					TransportMode.car, 
					offer.getAccess().getStation().facility(), 
					newFS.facility(), 
					offer.getDepartureTime() + offer.getAccess().getTravelTime() + offer.getAccess().getOffsetDur(), 
					offer.getDemand().getAgent().getPerson());
			CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(offer, CarsharingOffer.SUCCESS_FREEFLOATINGOFFER);
			builder.setDrive(offer.getNbOfVehicles(), elements);
			builder.setEgress((CarsharingStationMobsim) newFS, duration, distance, this.manager.getConfig().getInteractionOffset());
			return builder.build();
	}
	
	
	/**
	 * 
	 * @param demand
	 * @param stationTypes
	 * @param excludedDepartureStation
	 * @return
	 */
	public Map<String, SelectedStation> getClosestStationToArrivalLink(CarsharingDemand demand, CarsharingStationMobsim excludedDepartureStation) {
		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(
							demand.getDestination().getCoord().getX(), demand.getDestination().getCoord().getY(), 
							manager.getConfig().getSearchDistance());
		
		if(stations.isEmpty()) return null;
		
		Map<String, SelectedStation> closestStations = new HashMap<String, SelectedStation>();
		for(CarsharingStationMobsim station: stations) {
			if(station.equals(excludedDepartureStation) || station.getType().equals(FLOATING_STATION)) continue;
			final double egressDist = NetworkUtils.getEuclideanDistance(demand.getDestination().getCoord(), station.facility().getCoord());
			final double traveltime = egressDist * this.beelineWalkSpeed;
			final double distance = egressDist * this.beelineDistanceFactor;	
			//double stationTime = demand.getArrivalTime() - traveltime;
			CarsharingBookingStation bs = manager.booking().track(station);
			SelectedStation standardStation = closestStations.get(STANDARD_STATION);
			//int nPark = bs.parkingAvailability(stationTime); // estimate
			int nPark = bs.parkingAvailability();
			if(nPark >= demand.getNbrOfVeh()) { // success
				if(standardStation == null || standardStation.failure || standardStation.distance > distance) {
					closestStations.put(STANDARD_STATION, new SelectedStation(station, traveltime, distance));
				}
			} else { // Failure
				closestStations.put(STANDARD_STATION, new SelectedStation(true));
			}
		}
		return closestStations;
	}
	
	
	/**
	 * 
	 * @param demand
	 * @param stationTypes
	 * @return
	 */
	public Map<String, SelectedStation> getClosestStationToDepartureLink(CarsharingDemand demand) {

		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(
							demand.getOrigin().getCoord().getX(), 
							demand.getOrigin().getCoord().getY(), 
							manager.getConfig().getSearchDistance());
		
		if(stations.isEmpty()) {
			return null;
		} 
		
		Map<String, SelectedStation> closestStations = new HashMap<String, SelectedStation>();
		for(CarsharingStationMobsim station: stations) {
			
			final double accessDist = NetworkUtils.getEuclideanDistance(
					demand.getOrigin().getCoord(), 
					station.facility().getCoord());
			final double traveltime = accessDist * this.beelineWalkSpeed;
			final double distance = accessDist * this.beelineDistanceFactor;	
			//double stationTime = this.time + traveltime;
			
			SelectedStation s = closestStations.get(STANDARD_STATION);
			String type = STANDARD_STATION;
			if(station.getType().equals(FLOATING_STATION)) {
				s = closestStations.get(FLOATING_STATION);
				type = FLOATING_STATION;
			} 
			
			CarsharingBookingStation bs = manager.booking().track(station);
			//int nVeh = bs.vehicleAvailability(stationTime); // estimate
			int nVeh = bs.vehicleAvailability();
			if(nVeh >= demand.getNbrOfVeh()) { // success
				if(s == null || s.failure || s.distance > distance) {
					closestStations.put(type, new SelectedStation(station, traveltime, distance));
				}
			} else { // Failure
				closestStations.put(type, new SelectedStation(true));
			}
		}
		
		return closestStations;
		
	}
		
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	protected class SelectedStation {
		public SelectedStation() {
			failure = false; station = null; distance = Double.NaN; traveltime = Double.NaN;
		}
		public SelectedStation(CarsharingStationMobsim station, double traveltime, double distance) {
			failure = false; this.station = station; this.distance = distance; this.traveltime = traveltime;
		}
		public SelectedStation(boolean failure) {
			this();
			this.failure = failure;
		}
		public CarsharingStationMobsim station;
		public double traveltime;
		public double distance;
		public double temperature;
		public boolean failure;
	}
	
}
