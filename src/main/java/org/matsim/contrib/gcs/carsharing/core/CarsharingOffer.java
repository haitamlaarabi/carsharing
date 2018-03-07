package org.matsim.contrib.gcs.carsharing.core;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;


public class CarsharingOffer {
	
	// ***** The Builder *****
	public static class Builder {

		public static Builder newInstanceFromAgent(CarsharingAgent agent) {
			return new Builder(agent, null, null);
		}
		public static Builder newInstanceFromOffer(CarsharingOffer o, String status) {
			return new Builder(o, status);
		}
		public static Builder newInstanceFromDemand(CarsharingDemand demand, String status) {
			return new Builder(demand.getAgent(), demand, null);
		}
		
		CarsharingAgent agent;
		CarsharingDemand demand;
		AccessOffer access;
		EgressOffer egress;
		DriveOffer drive;
		String status;
		double cost;
		
		private Builder(CarsharingOffer o, String status) {
			super();
			this.agent = o.agent;
			this.demand = o.demand;
			this.access = new AccessOffer(o.access);
			this.drive = new DriveOffer(o.drive);
			this.egress = new EgressOffer(o.egress);
			this.cost = o.cost;
			this.status = status;
		}
		
		private Builder(CarsharingAgent agent, CarsharingDemand demand, String failureStatus) {
			super();
			this.demand = demand;
			this.agent = agent;
			this.status = failureStatus;
			this.status = null;
			this.access = null;
			this.egress = null;
			this.drive = null;
			this.cost = 0;
		}

		public void setAccess(double depTime, CarsharingStationMobsim aStation, double aTT, double aDist) {
			access = new AccessOffer(depTime, aStation, aTT, aDist);
		}
		
		public void setAccess(String failureMsg) {
			access = new AccessOffer(failureMsg);
		}
		
		public void setEgress(CarsharingStationMobsim eStation, double eTT, double eDist) {
			egress = new EgressOffer(drive.time+drive.travelTime, eStation, eTT, eDist);
		}
		
		public void setEgress(double deptime, CarsharingStationMobsim eStation, double eTT, double eDist) {
			egress = new EgressOffer(deptime, eStation, eTT, eDist);
		}
		
		public void setEgress(String failureMsg) {
			egress = new EgressOffer(failureMsg);
		}
		
		public void setDrive(int nVEH) {
			if(this.drive == null) {
				drive = new DriveOffer(access.time+access.travelTime, nVEH, null);
			}
			drive.nVEH = nVEH;
		}
		
		public void setDrive(int nVEH, RouteData rd) {
			drive = new DriveOffer(access.time+access.travelTime, nVEH, rd);
		}
		
		public void setCost(double cost) {
			this.cost = cost;
		}
						
		public CarsharingOffer build(){
			if(drive == null) drive = new DriveOffer(0, 0, null);
			if(egress == null) egress = new EgressOffer(0, null, 0, 0);
			if(access == null) access = new AccessOffer(0, null, 0, 0);
			CarsharingOffer o = new CarsharingOffer(this);
			o.setCost(this.cost);
			return o;
		}
	}
	
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class AccessOffer {

		private CarsharingStationMobsim station;
		private double travelTime;
		private double distance;
		private double time;
		private String status;
		public AccessOffer(String failureMsg) {
			this.status = failureMsg;
		}
		public AccessOffer(double depTime, CarsharingStationMobsim aStation, double aDur, double aDist) {
			this.station = aStation;
			this.distance = aDist;
			this.travelTime = aDur;
			this.time = depTime;
			this.status = null;
		}
		AccessOffer(AccessOffer a) {
			this.station = a.station;
			this.distance = a.distance;
			this.travelTime = a.travelTime;
			this.time = a.time;
			this.status = a.status;
		}
		public CarsharingStationMobsim getStation() { return station; }
		public double getTravelTime() {	return travelTime; }
		public double getDistance() { return distance; }
		public String getStatus() { return status; }
		public double getTime() { return this.time; }
	}
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class EgressOffer  {
		private CarsharingStationMobsim station;
		private double travelTime;
		private double distance;
		private String status;
		private double time;
		public EgressOffer(String failureMsg) {
			this.status = failureMsg;
		}
		public EgressOffer(double depTime, CarsharingStationMobsim eStation, double eDur, double eDist) {
			this.station = eStation;
			this.travelTime = eDur;
			this.distance = eDist;
			this.time = depTime;
			this.status = null;
		}
		EgressOffer(EgressOffer e) {
			this.station = e.station;
			this.travelTime = e.travelTime;
			this.distance = e.distance;
			this.time = e.time;
			this.status = e.status;
		}
		public CarsharingStationMobsim getStation() { return station; }
		public double getTravelTime() { return travelTime; }
		public double getDistance() { return distance; }
		public String getStatus() { return status; }
		public double getTime() { return this.time; }
	}
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class DriveOffer {
		private double time;
		private double travelTime = 0;
		private double distance = 0;
		private double offset = 0;
		private List<? extends PlanElement> route = null;
		private int nVEH;
		public DriveOffer(double depTime, int nVEH, RouteData rd){
			this.time = depTime;
			this.nVEH = nVEH;
			if(rd != null) {
				this.route = rd.path; 
				this.distance = rd.distance;
				this.travelTime = rd.time;
				this.offset = rd.offset;
			}
		}
		DriveOffer(DriveOffer d) {
			this.time = d.time;
			this.nVEH = d.nVEH;
			this.route = d.route;
			this.distance = d.distance;
			this.travelTime = d.travelTime;
			this.offset = d.offset;
		}
		public double getRentalTime() { return travelTime + 2*this.offset; }
		public double getDistance() { return distance; }
		public List<? extends PlanElement> getRoute() { return this.route; }
		public double getTime() { return this.time; }
		public double getOffset() { return this.offset; }
		public int getNbVehicles() { return this.nVEH; }
	}
	
	/**
	 * 
	 * @param builder
	 */
	public CarsharingOffer(Builder builder) {
		this.demand = builder.demand;
		this.access = builder.access;
		this.egress = builder.egress;
		this.drive = builder.drive;
		this.agent = builder.agent;	
	}
	
	
	// ************
	
	public CarsharingDemand getDemand() { return this.demand; }
	public AccessOffer getAccess() { return this.access; }
	public EgressOffer getEgress() { return this.egress; }
	public DriveOffer getDrive() { return this.drive; }
	public int getNbOfVehicles() { return this.drive.nVEH; }
	public boolean hasValidAccess() { return this.access.status == null; }
	public boolean hasValidEgress() { return this.egress.status == null; }
	public CarsharingAgent getAgent() { return this.agent; }
	public double getCost() { return this.cost; }
	public double getDepartureTime() { return this.access.time; }
	public double getArrivalTime() { return this.egress.time + this.egress.travelTime; }
	public double getAccessTime() { return this.access.time + this.access.travelTime; }
	public double getEgressTime() { return this.egress.time; }
	
	
	public void setCost(double c) { this.cost = c; }
	
	// ************
	private final CarsharingAgent agent;
	private final CarsharingDemand demand;
	private final AccessOffer access;
	private final EgressOffer egress;
	private final DriveOffer drive;
	private double cost;
	private int offset;
	
	
	// ***********
	public static final String FAILURE_NODEPARTURESTATION = "no_departure_station";
	public static final String FAILURE_NOARRIVALSTATION = "no_arrival_station";
	public static final String FAILURE_NODEPARTUREAVAILABILITY = "no_departure_availability";
	public static final String FAILURE_NOARRIVALAVAILABILITY = "no_arrival_availability";
	public static final String FAILURE_FLOATINGLIMIT = "floating_limit";
	public static final String SUCCESS_STANDARDOFFER = "standard_offer";
	public static final String SUCCESS_FREEFLOATINGOFFER = "freefloating_offer";
	public static final String FAILURE_NOOFFER = "no_offer";
	public static final String FAILURE_WALK_OFFER = "walk_offer";
		
}
