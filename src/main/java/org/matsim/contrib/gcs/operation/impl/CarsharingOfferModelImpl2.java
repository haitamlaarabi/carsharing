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
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class CarsharingOfferModelImpl2 implements CarsharingOfferModel  {

	// Attribute
	Scenario scenario;
	CarsharingManager manager;
	CarsharingConfigGroup cs_conf;
	
	Network network;
	boolean floatingStations;
	double tau = 2.0 * 3600.0;
	double alpha = 0.7;
	private double timeFeePerMinute; // 1 euro per minute
	private double time;
	
	protected static String STANDARD_STATION = "STANDARD";
	protected static String FLOATING_STATION = "FLOATING";
	
	private TripRouter router;
	
	
	@Inject
	CarsharingOfferModelImpl2(Scenario scenario, CarsharingManager manager, Provider<TripRouter> tripRouterProvider) {
		this.scenario = scenario;
		this.manager = manager;
		this.cs_conf = manager.getConfig();
		this.network = this.scenario.getNetwork();
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
		boolean better_walk = false;
		for(CarsharingOffer depOffer : calculateDepartureOffers(demand)) {
			if(depOffer.hasValidAccess()) {
				for(CarsharingOffer arrOffer : calculateArrivalOffers(depOffer)) {
					if(arrOffer.hasValidEgress()) {
						/*final double trip_distance = arrOffer.getAccess().getDistance() + arrOffer.getDrive().getDistance() + arrOffer.getEgress().getDistance();
						final double walk_distance = CarsharingUtils.distanceBeeline(
								NetworkUtils.getEuclideanDistance(demand.getOrigin().getCoord(), demand.getDestination().getCoord()), 
								this.scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(TransportMode.walk));
						if(trip_distance <= walk_distance + 1000.0) {
							better_walk = true;
						} else {*/
							offers.add(arrOffer);
						//}
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
		}
		return offers;
	}
	
	// ***********************************************************************************
	// ***********************************************************************************
	
	public ArrayList<CarsharingOffer> calculateDepartureOffers(CarsharingDemand demand) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		ArrayList<SelectedStation> closestStation = this.getClosestStationToDepartureLink(demand);
		if (closestStation == null || closestStation.isEmpty()) { 
			offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_NODEPARTURESTATION));
		} else {
			for(SelectedStation ss : closestStation) {
				if(ss.isFloating) { // FLOATING OFFERS
					if(ss.station != null) {
						offers.add(this.getAccessStationStandardOffer(demand, ss, 0, ss.station.parking().getFleetSize()));
					} else {
						offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_FLOATINGLIMIT));
					}
				} else { // STANDARD OFFERS
					if(ss.station != null) {
						offers.add(this.getAccessStationStandardOffer(demand, ss, 0, demand.getNbrOfVeh()));
					} else {
						offers.add(this.getAccessFailureOffer(demand, CarsharingOffer.FAILURE_NODEPARTUREAVAILABILITY));
					}
				}
			}
		}
		return offers;
	}

	
	/**
	 * calculateArrivalOffers
	 */
	public ArrayList<CarsharingOffer> calculateArrivalOffers(CarsharingOffer offer) {
		ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
		ArrayList<SelectedStation> closestStation = this.getClosestStationToArrivalLink(offer.getDemand(), offer.getAccess().getStation());
		if (closestStation == null || closestStation.isEmpty()) { 
			offers.add(getEgressFailureOffer(offer.getDemand(), CarsharingOffer.FAILURE_NOARRIVALSTATION));
		} else {
			for(SelectedStation ss : closestStation) {
				if(ss.station != null) {
					offers.add(this.getEgressStationStandardOffer(offer, ss));
				} else  {
					offers.add(this.getEgressFailureOffer(offer.getDemand(), CarsharingOffer.FAILURE_NOARRIVALAVAILABILITY));
				}
			}
		}
		// FLOATING OFFER
		if(this.floatingStations) 
			offers.add(this.getEgressStationFloatingOffer(offer));
		// USER RELOCATION TIME
		offers.addAll(manager.relocation().relocationList(time, offer.getDemand(), offers));
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
	
	public CarsharingOffer getWalkOffer(CarsharingDemand demand, String walkMsg) {
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromDemand(demand, walkMsg);
		builder.setAccess(walkMsg);
		builder.setEgress(walkMsg);
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
		builder.setAccess(this.time, selectedStation.station, selectedStation.traveltime, selectedStation.distance);
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
		
		RouteData rd = CarsharingRouterUtils.calcTCC(manager, 
				o.getAccess().getStation().facility(), 
				s.station.facility(), 
				o.getAccessTime(), 
				o.getDemand().getAgent().getPerson());
		
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(o, CarsharingOffer.SUCCESS_STANDARDOFFER);
		builder.setDrive(o.getNbOfVehicles(), rd);
		builder.setEgress(s.station, s.traveltime, s.distance);
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
			
			final double egressDist = NetworkUtils.getEuclideanDistance(newFS.facility().getCoord(), offer.getDemand().getDestination().getCoord());
			final double traveltime = CarsharingUtils.travelTimeBeeline(egressDist, this.cs_conf.getEgressWalkCalcRoute());
			final double distance = CarsharingUtils.distanceBeeline(egressDist, this.cs_conf.getEgressWalkCalcRoute());
			
			RouteData rd = CarsharingRouterUtils.calcTCC(manager, 
					offer.getAccess().getStation().facility(), 
					newFS.facility(), 
					offer.getAccessTime(), 
					offer.getDemand().getAgent().getPerson());
			
			CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromOffer(offer, CarsharingOffer.SUCCESS_FREEFLOATINGOFFER);
			builder.setDrive(offer.getNbOfVehicles(), rd);
			builder.setEgress((CarsharingStationMobsim) newFS, traveltime, distance);
			return builder.build();
	}
	
	
	/**
	 * 
	 * @param demand
	 * @param stationTypes
	 * @param excludedDepartureStation
	 * @return
	 */
	public ArrayList<SelectedStation> getClosestStationToArrivalLink(CarsharingDemand demand, CarsharingStationMobsim excludedDepartureStation) {
		double euc_distance = manager.getConfig().getSearchDistance()/this.cs_conf.getEgressWalkCalcRoute().getBeelineDistanceFactor();
		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(
							demand.getDestination().getCoord().getX(), demand.getDestination().getCoord().getY(), 
							euc_distance);
		
		if(stations.isEmpty()) return null;
		
		ArrayList<SelectedStation> closestStations = new ArrayList<SelectedStation>();
		for(CarsharingStationMobsim station: stations) {
			if(station.equals(excludedDepartureStation) || station.getType().equals(FLOATING_STATION)) continue;
			final double egress_euc_dist = NetworkUtils.getEuclideanDistance(demand.getDestination().getCoord(), station.facility().getCoord());
			
			CarsharingBookingStation bs = manager.booking().track(station);
			int nPark = bs.parkingAvailability();
			if(nPark >= demand.getNbrOfVeh()) { // success
				final double traveltime = CarsharingUtils.travelTimeBeeline(egress_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
				final double distance = CarsharingUtils.distanceBeeline(egress_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
				closestStations.add(new SelectedStation(station, traveltime, distance, false));
			} else { // Failure
				closestStations.add(new SelectedStation(true));
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
	public ArrayList<SelectedStation> getClosestStationToDepartureLink(CarsharingDemand demand) {
		double euc_distance = manager.getConfig().getSearchDistance()/this.cs_conf.getAccessWalkCalcRoute().getBeelineDistanceFactor();
		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(
							demand.getOrigin().getCoord().getX(), demand.getOrigin().getCoord().getY(), 
							euc_distance);
		
		if(stations.isEmpty()) return null;
		
		ArrayList<SelectedStation> closestStations = new ArrayList<SelectedStation>();
		for(CarsharingStationMobsim station: stations) {
			final double access_euc_dist = NetworkUtils.getEuclideanDistance(demand.getOrigin().getCoord(), station.facility().getCoord());

			CarsharingBookingStation bs = manager.booking().track(station);
			int nVeh = bs.vehicleAvailability();
			if(nVeh >= demand.getNbrOfVeh()) { // success
				final double traveltime = CarsharingUtils.travelTimeBeeline(access_euc_dist, this.cs_conf.getAccessWalkCalcRoute());
				final double distance = CarsharingUtils.distanceBeeline(access_euc_dist, this.cs_conf.getAccessWalkCalcRoute());
				closestStations.add(new SelectedStation(station, traveltime, distance, station.getType().equals(FLOATING_STATION)));
			} else { // Failure
				closestStations.add(new SelectedStation(true));
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
			failure = false; 
			station = null; 
			distance = Double.NaN; 
			traveltime = Double.NaN;
		}
		public SelectedStation(CarsharingStationMobsim station, double traveltime, double distance, boolean isFLoating) {
			failure = false; 
			this.station = station; 
			this.distance = distance; 
			this.traveltime = traveltime;
			this.isFloating = isFLoating;
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
		public boolean isFloating;
	}
	
}
