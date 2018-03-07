package org.matsim.contrib.gcs.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public final class CarsharingUtils {
	
	static String ACCESS_STATION = "access_station";
	static String EGRESS_STATION = "egress_station";
	
	

	public static double distanceBeeline(double euc_distance, ModeRoutingParams r_param) {
		double distance = euc_distance * r_param.getBeelineDistanceFactor();
		return distance;
	}
	
	public static double travelTimeBeeline(double euc_distance, ModeRoutingParams r_param) {
		double traveltime = distanceBeeline(euc_distance, r_param) / r_param.getTeleportedModeSpeed();
		return traveltime;
	}
	
	public static boolean isUnRoutedCarsharingLeg(PlanElement pe) {
		if(pe instanceof Leg) {
			return 	((Leg)pe).getMode().equals(CarsharingPlanModeCst.ongoingTrip) ||
					((Leg)pe).getMode().equals(CarsharingPlanModeCst.startTrip) ||
					((Leg)pe).getMode().equals(CarsharingPlanModeCst.endTrip) ||
					((Leg)pe).getMode().equals(CarsharingPlanModeCst.directTrip);
		}
		return false;
	}
	
	public static boolean isRoutedCarsharingLeg(PlanElement pe) {
		if(pe instanceof Leg) {
			if(	((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_access_walk) || 
				((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_drive) ||
				((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_egress_walk)) {
				return true;
			}
		} else {
			if(((Activity)pe).getType().equals(CarsharingRouterUtils.ACTIVITY_TYPE_NAME)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCarsharingElement(PlanElement pe) {
		if(pe instanceof Leg) {
			if(	((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_access_walk) || 
				((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_drive) ||
				((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_egress_walk) ||
				((Leg)pe).getMode().equals(CarsharingPlanModeCst.directTrip) ) {
				return true;
			}
		} else {
			if(((Activity)pe).getType().equals(CarsharingRouterUtils.ACTIVITY_TYPE_NAME)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isAccessWalk(PlanElement pe) {
		return (pe instanceof Leg && ((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_access_walk));
	}
	
	public static boolean isDrive(PlanElement pe) {
		return (pe instanceof Leg && ((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_drive));
	}
	
	public static boolean isEgressWalk(PlanElement pe) {
		return (pe instanceof Leg && ((Leg)pe).getMode().equals(CarsharingRouterUtils.cs_egress_walk));
	}
	
	public static boolean isAccessStation(PlanElement pe) {
		return (pe instanceof Activity && 
				((Activity)pe).getType().equals(CarsharingRouterUtils.ACTIVITY_TYPE_NAME) &&
				((Activity)pe).getAttributes().getAttribute(ACCESS_STATION) != null);
	} 
	
	public static boolean isEgressStation(PlanElement pe) {
		return (pe instanceof Activity && 
				((Activity)pe).getType().equals(CarsharingRouterUtils.ACTIVITY_TYPE_NAME) &&
				((Activity)pe).getAttributes().getAttribute(EGRESS_STATION) != null);
	} 
	
	
	public static Activity createAccessStationActivity(Facility facility, double departureTime, double offset) {
		Activity act = createStationActivity(facility, departureTime, offset);
		act.getAttributes().putAttribute(ACCESS_STATION, true);
		return act;
	}
	
	public static Activity createEgressStationActivity(Facility facility, double arrivalTime, double offset) {
		Activity act = createStationActivity(facility, arrivalTime, offset);
		act.getAttributes().putAttribute(EGRESS_STATION, true);
		return act;
	}
	
	private static Activity createStationActivity(Facility facility, double time, double offset) {
		Activity act = PopulationUtils.createActivityFromCoord(CarsharingRouterUtils.ACTIVITY_TYPE_NAME, facility.getCoord());
		act.setStartTime(time);
		act.setMaximumDuration(offset);
		act.setFacilityId(facility.getId());
		act.setLinkId(facility.getLinkId());
		return act;
	}
	
	public static Leg createDriveLeg(Facility startFacility, Facility endFacility, List<? extends PlanElement> peroute, Id<Vehicle> idVehicle) {
		NetworkRoute dRoute = new LinkNetworkRouteImpl(startFacility.getLinkId(), CarsharingUtils.getLinks(peroute), endFacility.getLinkId());
		dRoute.setDistance(CarsharingUtils.calcDistance(peroute));
		dRoute.setTravelTime(CarsharingUtils.calcDuration(peroute));
		dRoute.setVehicleId(idVehicle);
		Leg driveLeg = PopulationUtils.createLeg(CarsharingRouterUtils.cs_drive);
		driveLeg.setTravelTime(dRoute.getTravelTime());
		driveLeg.setRoute(dRoute);
		return driveLeg;
	}
	
	public static Leg createWalkLeg(String walkmode, Facility startFacility, Facility endFacility, double traveltime, double distance) {
		NetworkRoute awalkRoute = new LinkNetworkRouteImpl(startFacility.getLinkId(), endFacility.getLinkId());
		awalkRoute.setTravelTime(traveltime);
		awalkRoute.setDistance(distance);
		final Leg walkLeg = PopulationUtils.createLeg(walkmode);
		//aWalkLeg.setDepartureTime(departureTime);
		walkLeg.setTravelTime(awalkRoute.getTravelTime());
		walkLeg.setRoute(awalkRoute);
		return walkLeg;
	}
	
	
	public static ActivityParams createActivityParam(String TYPE, double oTime, double cTime, double tDuration) {
		ActivityParams x = new ActivityParams(TYPE);
		x.setClosingTime(cTime);
		x.setOpeningTime(oTime);
		x.setTypicalDuration(tDuration);
		return x;
	}
	
	public static ModeParams createModeParam(String mode, double factTraveling, double factMoney, double factConstant) {
		double utilPerf = 6.0;
		ModeParams x = new ModeParams(mode);
		x.setMode(mode);
		x.setMarginalUtilityOfTraveling(factTraveling*60.0 + utilPerf);
		//x.setMarginalUtilityOfDistance(factDistance/1000.0 + utilPerf);
		x.setMonetaryDistanceRate(factMoney/1000.0);
		x.setConstant(factConstant);
		return x;
	}
	
	public static ModeRoutingParams createModeRouting(String mode, double distanceFact, double teleSpeed) {
		ModeRoutingParams x = new ModeRoutingParams(mode);
		x.setMode(mode);
		x.setBeelineDistanceFactor(distanceFact);
		//x.setTeleportedModeFreespeedFactor(teleFreeSpeedFact);
		x.setTeleportedModeSpeed(teleSpeed/3.6);
		return x;
	}
	
	
	/*private Facility[] extractODFacilitiesOfCarsharing(Leg carsharingLeg) {
		Facility OFacility, DFacility;
		try {
			List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
			OFacility = CarsharingUtils.getDummyFacility((Activity)planElements.get(planElements.indexOf(carsharingLeg) - 1));
			DFacility = CarsharingUtils.getDummyFacility((Activity) planElements.get(planElements.indexOf(carsharingLeg) + 1));
		} catch(Exception e) {
			throw new RuntimeException("Inconsistent plan structure with what carsharing route builder expects.\n" + e.getMessage());
		}
		return new Facility[] { OFacility , DFacility };
	}*/
	
	public static boolean isNaNorInfinit(double nbr) {
		return Double.isNaN(nbr) || Double.isInfinite(nbr);
	}
	
	// 1kwh = 1000 * 3600 Joule
	// 1joule = 1/1000 kW * 1/3600 h
	public static Double tokiloWattHour(double joule){ return (joule/3600000); }
	
	
	public static DecimalFormat kWh_unit = new DecimalFormat("#.00000");
	
	
	public static String formatTime(int year, int month, int day, DateFormat df, double seconds) {
		Calendar cal = new GregorianCalendar(year, month, day); 
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, (int)seconds);
		cal.set(Calendar.MILLISECOND, 0);
		return df.format(cal.getTime());
	}
	
	

	
	/**
	 * 
	 * @param trip
	 * @param endExclusive
	 * @return
	 */
	public static double calcDuration(final List<? extends PlanElement> trip) {
		double tt = 0;

		for ( PlanElement pe : trip ) {
			
			/*if(endExclusive != null && pe.equals(endExclusive))
				break;*/
			
			if ( pe instanceof Leg ) {
				final double curr = ((Leg) pe).getTravelTime();
				if ( curr == Time.UNDEFINED_TIME ) 
					throw new RuntimeException( pe+" has not travel time" );
				tt += curr;
			}

			/*if ( pe instanceof Activity ) {
				final double dur = ((Activity) pe).getEndTime();
				if ( dur != Time.UNDEFINED_TIME ) {
					tt = dur;
				}
			}*/

		}

		return tt;
	}
	
	
	public static List<Id<Link>> getLinks(final List<? extends PlanElement> trip) {
		ArrayList<Id<Link>> links = new ArrayList<>();
		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe;
				NetworkRoute nr = (NetworkRoute) leg.getRoute();
				links.addAll(nr.getLinkIds());
			}
		}
		return links;
	}
	
	
	
	/**
	 * 
	 * @param trip
	 * @return
	 */
	/*public static double calcDuration(final List<? extends PlanElement> trip) {
		return calcDuration(trip, null);
	}*/
	
	
	/**
	 * 
	 * @param trip
	 * @return
	 */
	public static double calcDistance(final List<? extends PlanElement> trip) {
		
		if(trip == null) return Double.NaN;
		double td = 0;

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				final double curr = ((Leg) pe).getRoute().getDistance();
				if ( curr == Double.NaN ) throw new RuntimeException( pe+" has not travel distance" );
				td += curr;
			}
		}
		return td;
	}
	

	/*public static double calcDistance(final Path path) {
		double td = 0;
		for ( Link link : path.links ) {
			td += link.getLength();
		}
		return td;
	}*/
	

	public static long toSecond(long h, long m, long s) {
		return h * 3600 + m * 60 + s;
	}
	
	/*public static double calcCost(final List<? extends PlanElement> trip, CarsharingLegScoringFunction scoreFunc) {
		double cost = 0;
		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) {
				final Leg leg = (Leg) pe;
				final double time = leg.getTravelTime();
				if ( time == Time.UNDEFINED_TIME ) throw new RuntimeException( pe+" has not travel time" );
				// XXX no distance!
				cost += scoreFunc.calcLegScore(leg.getDepartureTime(), leg.getDepartureTime()+leg.getTravelTime(), leg);
			}
		}
		return cost;
	}*/
	
	
	public static Facility getDummyFacility(final Activity activity) {
		return getDummyFacility(activity.getCoord(), activity.getLinkId(), "dummy");
	}
	
	public static Facility getDummyFacility(final Coord coordinate, final Id<Link> link_Id, final String id) {
		return( new Facility () {
				private final Coord coord = coordinate;
				private final Id<Link> linkId = link_Id;
				private final Map<String, Object> customAttributes = new LinkedHashMap<String, Object>();
				private final Id<ActivityFacility> fid = Id.create(id, ActivityFacility.class);
				@Override
				public Coord getCoord() { return coord; }
				@Override
				public Id<ActivityFacility> getId() {	return null; }
				@Override
				public Map<String, Object> getCustomAttributes() { return customAttributes; }
				@Override
				public Id getLinkId() { return linkId; }
				@Override
				public String toString() { return "[coord=" + coord.toString() + "] [linkId=" + linkId + "]"; }
			});
	}
	
	
	public static int chooseProbability(double[] probas) {
		double rnd = MatsimRandom.getRandom().nextDouble();
		double sum = 0.0;
		for(int i = 0; i < probas.length; i++) {
			sum += probas[i];
			if (rnd <= sum) {
				return i;
			}
		}
		return -1;
	}
	
	
}
