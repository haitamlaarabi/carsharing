package org.matsim.contrib.gcs.router;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.Facility;

public final class CarsharingRouterUtils {

	public static final String cs_drive = "cs_drive";
	public static final String cs_access_walk = "cs_access_walk";
	public static final String cs_egress_walk = "cs_egress_walk";
	
	
	public static final String ACTIVITY_TYPE_NAME = "cs_station";
	
	
	public static final String cs_walk = "cs_walk";
	public static final String cs_pt = "cs_pt";

	
	public static class RouteData {
		public List<? extends PlanElement> path = null;
		public double distance = 0;
		public double time = 0;
	}
	
	public static RouteData calcTCC(CarsharingManager m, Facility o, Facility d, double deptime, Person p) {
		RouteData rd = new RouteData();
		rd.path = m.router().calcRoute(CarsharingRouterUtils.cs_drive, o, d, deptime, p);
		NetworkRoute nr = ((NetworkRoute)((Leg)rd.path.get(0)).getRoute());
		for(Id<Link> linkid : nr.getLinkIds()) {
			Link tempL = m.getCarNetwork().getLinks().get(linkid);
			rd.time += m.ttc().getLinkTravelTime(tempL, deptime + rd.time, null, null);
			rd.distance += tempL.getLength();
		}
		Link tempL = m.getCarNetwork().getLinks().get(d.getLinkId());
		rd.time += m.ttc().getLinkTravelTime(tempL, deptime + rd.time, null, null);
		rd.distance += tempL.getLength();
		rd.time += 2*m.getConfig().getInteractionOffset();
		return rd;
	}
	
	public static RouteData calcTCCNoOffset(CarsharingManager m, Facility o, Facility d, double deptime, Person p) {
		RouteData rd = new RouteData();
		rd.path = m.router().calcRoute(CarsharingRouterUtils.cs_drive, o, d, deptime, p);
		NetworkRoute nr = ((NetworkRoute)((Leg)rd.path.get(0)).getRoute());
		for(Id<Link> linkid : nr.getLinkIds()) {
			Link tempL = m.getCarNetwork().getLinks().get(linkid);
			rd.time += m.ttc().getLinkTravelTime(tempL, deptime + rd.time, null, null);
			rd.distance += tempL.getLength();
		}
		Link tempL = m.getCarNetwork().getLinks().get(d.getLinkId());
		rd.time += m.ttc().getLinkTravelTime(tempL, deptime + rd.time, null, null);
		rd.distance += tempL.getLength();
		return rd;
	}
}
