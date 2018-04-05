package org.matsim.contrib.gcs.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;

public class CarsharingNearestStationRouterModule extends CarsharingDefaultRouterModule {

	final double searchDistance;
	final QuadTree<CarsharingStationMobsim> qt;
	final CarsharingManager m;
	final CarsharingConfigGroup cs_conf;
	
	public CarsharingNearestStationRouterModule(Scenario scenario, CarsharingManager manager, String cssMode) {
		super(scenario, manager, cssMode);
		this.searchDistance = manager.getConfig().getSearchDistance();
		this.qt = manager.getStations().qtree();
		this.m = manager;
		this.cs_conf = m.getConfig();
	}
	
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {	
		
		Network network = scenario.getNetwork();
		LeastCostPathCalculatorFactory leastCostAlgoFactory = new DijkstraFactory();
        final OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
        final FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		final List<PlanElement> trip = new ArrayList<PlanElement>();	
		boolean aStation = (cssMode.equals(CarsharingPlanModeCst.directTrip) || cssMode.equals(CarsharingPlanModeCst.startTrip));
		boolean eStation = (cssMode.equals(CarsharingPlanModeCst.directTrip) || cssMode.equals(CarsharingPlanModeCst.endTrip));
		
		// *********************************************
		// NEAREST STATIONS
		CarsharingLocationInfo pickupLocation = buildDepartureLocation(fromFacility, aStation);
		CarsharingLocationInfo dropoffLocation = buildArrivalLocation(toFacility, pickupLocation.station, eStation);
		if(pickupLocation.station == null || dropoffLocation.station == null) { 
			return new ArrayList<PlanElement>();
		} 
		// **************************************************
		
		//  **************************************************
		// ROUTING
		double routetime = departureTime;
		// Walk
		trip.add(CarsharingUtils.createWalkLeg(
				CarsharingRouterUtils.cs_access_walk, 
				fromFacility, pickupLocation.facility, 
				pickupLocation.traveltime, 
				pickupLocation.distance));
		routetime += pickupLocation.traveltime;
		
		// Access
		if(aStation) {
			Activity a = CarsharingUtils.createAccessStationActivity(pickupLocation.facility, routetime, this.manager.getConfig().getInteractionOffset());
			trip.add(a);
		}
		
		RouteData rd = CarsharingRouterUtils.calcTCC(m, pickupLocation.facility, dropoffLocation.facility, routetime, person);
		// drive
		/*List<? extends PlanElement> pes = DefaultRoutingModules.createPureNetworkRouter(
				CarsharingRouterUtils.cs_drive, 
				scenario.getPopulation().getFactory(),
          		network,
          		leastCostAlgoFactory.createPathCalculator(network, disutilityFactory.createTravelDisutility(travelTime), travelTime)
          		).calcRoute(pickupLocation.facility, dropoffLocation.facility, routetime, person);*/
		trip.add(CarsharingUtils.createDriveLeg(pickupLocation.facility, dropoffLocation.facility, rd.path, null));
		NetworkRoute NR = ((NetworkRoute)((Leg)trip.get(trip.size()-1)).getRoute());
		NR.setDistance(rd.distance);
		NR.setTravelTime(rd.time);
		routetime += NR.getTravelTime() + 2*rd.offset;
		
		
		// Egress
		if(eStation) {
			Activity a = CarsharingUtils.createEgressStationActivity(dropoffLocation.facility, routetime, this.manager.getConfig().getInteractionOffset());
			trip.add(a);		
		}
		
		// Walk
		trip.add(CarsharingUtils.createWalkLeg(
				CarsharingRouterUtils.cs_egress_walk, 
				dropoffLocation.facility, 
				toFacility, 
				dropoffLocation.traveltime, 
				dropoffLocation.distance));
		routetime += dropoffLocation.traveltime;
		//  **************************************************
		
		return trip;
	}
	
	/**
	 * 
	 * @param coordinate
	 * @param departureTime
	 * @return
	 */
	
	private CarsharingLocationInfo buildDepartureLocation(Facility f, boolean hasStation) {
		CarsharingLocationInfo location = new CarsharingLocationInfo(f);
		if(hasStation) return location;
		return this.getNearestStationToDeparture(f);

	}
	
	private CarsharingLocationInfo buildArrivalLocation(Facility f, CarsharingStationMobsim s_toexclude, boolean hasStation) {
		CarsharingLocationInfo location = new CarsharingLocationInfo(f);
		if(hasStation) return location;
		return this.getNearestStationToArrival(f,  s_toexclude);
	} 
	

	public CarsharingLocationInfo getNearestStationToDeparture(Facility fromFacility) {
		CarsharingLocationInfo pickupLocation = this.getNearestStationToDeparture(fromFacility.getCoord());
		pickupLocation.facility = fromFacility;
		return pickupLocation;
	}
	
	public CarsharingLocationInfo getNearestStationToDeparture(Coord fromCoord) {
		CarsharingLocationInfo pickupLocation = new CarsharingLocationInfo(null);
		double euc_distance = manager.getConfig().getSearchDistance()/this.cs_conf.getAccessWalkCalcRoute().getBeelineDistanceFactor();
		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(fromCoord.getX(), fromCoord.getY(), euc_distance);
		for(CarsharingStationMobsim station: stations) {
			final double access_euc_dist = NetworkUtils.getEuclideanDistance(fromCoord, station.facility().getCoord());
			if(pickupLocation.station == null || pickupLocation.distance > access_euc_dist) {
				pickupLocation.station = station;
				pickupLocation.distance = CarsharingUtils.distanceBeeline(access_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
				pickupLocation.traveltime = CarsharingUtils.travelTimeBeeline(access_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
			}
		}
		return pickupLocation;
	}

	
	/**
	 * 
	 * @param coordinate
	 * @param arrivalTime
	 * @param excludedDepartureStation
	 * @return
	 */
	public CarsharingLocationInfo getNearestStationToArrival(Facility toFacility, CarsharingStationMobsim s_toexclude) {
		CarsharingLocationInfo dropoffLocation = this.getNearestStationToArrival(toFacility.getCoord(), s_toexclude);
		dropoffLocation.facility = toFacility;
		return dropoffLocation;
	}
	
	public CarsharingLocationInfo getNearestStationToArrival(Coord toCoord, CarsharingStationMobsim s_toexclude) {
		CarsharingLocationInfo dropoffLocation = new CarsharingLocationInfo(null);
		double euc_distance = manager.getConfig().getSearchDistance()/this.cs_conf.getEgressWalkCalcRoute().getBeelineDistanceFactor();
		Collection<CarsharingStationMobsim> stations = manager.getStations().qtree().getDisk(
				toCoord.getX(), toCoord.getY(), euc_distance);
		for(CarsharingStationMobsim station: stations) {
			if(station.equals(s_toexclude) || station.getType().equals("FLOATING")) continue;
			final double egress_euc_dist = NetworkUtils.getEuclideanDistance(toCoord, station.facility().getCoord());
			if(dropoffLocation.station == null || dropoffLocation.distance > egress_euc_dist) {
				dropoffLocation.station = station;
				dropoffLocation.distance = CarsharingUtils.distanceBeeline(egress_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
				dropoffLocation.traveltime = CarsharingUtils.travelTimeBeeline(egress_euc_dist, this.cs_conf.getEgressWalkCalcRoute());
			}
		}
		return dropoffLocation;
	}
	
	
	// *****************

	public static class CarsharingLocationInfo {
		CarsharingLocationInfo(Facility f, boolean floating) {
			this.facility = f;
			this.station = null;
			this.distance = 0;
			this.traveltime = 0;
			this.isFloating = floating;
		}
		CarsharingLocationInfo(Facility f) {
			this(f, false);
		}
		public CarsharingStationMobsim station;
		public Facility facility;
		public double distance;
		public double traveltime;
		public boolean isFloating;
	}
	
	
}
