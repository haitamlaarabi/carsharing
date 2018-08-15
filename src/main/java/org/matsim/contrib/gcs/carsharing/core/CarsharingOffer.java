package org.matsim.contrib.gcs.carsharing.core;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;


public class CarsharingOffer {
	
	// ***** The Builder *****
	public static class Builder {

		public static Builder newInstanceFromAgent(CarsharingAgent agent, CarsharingDemand demand) {
			return new Builder(agent, demand);
		}
		public static Builder newInstanceFromOffer(CarsharingOffer o) {
			return new Builder(o);
		}
		
		CarsharingAgent agent;
		CarsharingDemand demand;
		AccessOffer access;
		EgressOffer egress;
		DriveOffer drive;
		double cost;
		
		private Builder(CarsharingOffer o) {
			super();
			this.agent = o.agent;
			this.demand = o.demand;
			this.access = new AccessOffer(o.access);
			this.drive = new DriveOffer(o.drive);
			this.egress = new EgressOffer(o.egress);
			this.cost = o.cost;
		}
		
		private Builder(CarsharingAgent agent, CarsharingDemand demand) {
			super();
			this.demand = demand;
			this.agent = agent;
			this.access = null;
			this.egress = null;
			this.drive = null;
			this.cost = 0;
		}
		
		public void setAccess(CarsharingOfferStatus s) {
			access.status = s;
		}

		public void setAccess(int depTime, CarsharingStationMobsim aStation, int aTT, double aDist, CarsharingOfferStatus flag) {
			access = new AccessOffer(depTime, aStation, aTT, aDist, flag);
		}
		
		public void setEgress(CarsharingStationMobsim eStation, int eTT, double eDist, CarsharingOfferStatus flag) {
			egress = new EgressOffer(drive.time+drive.rentalTime, eStation, eTT, eDist, flag);
		}
		
		public void setEgress(int deptime, CarsharingStationMobsim eStation, int eTT, double eDist, CarsharingOfferStatus flag) {
			egress = new EgressOffer(deptime, eStation, eTT, eDist, flag);
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
			if(egress == null) egress = new EgressOffer(0, null, 0, 0, null);
			if(access == null) access = new AccessOffer(0, null, 0, 0, null);
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
		private int travelTime;
		private double distance;
		private int time;
		private CarsharingOfferStatus status;
		public AccessOffer(int depTime, CarsharingStationMobsim aStation, int aDur, double aDist, CarsharingOfferStatus flag) {
			this.station = aStation;
			this.distance = aDist;
			this.travelTime = aDur;
			this.time = depTime;
			this.status = flag;
		}
		AccessOffer(AccessOffer a) {
			this.station = a.station;
			this.distance = a.distance;
			this.travelTime = a.travelTime;
			this.time = a.time;
			this.status = a.status;
		}
		public CarsharingStationMobsim getStation() { return station; }
		public int getTravelTime() {	return travelTime; }
		public double getDistance() { return distance; }
		public CarsharingOfferStatus getStatus() { return status; }
		public int getTime() { return this.time; }
	}
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class EgressOffer  {
		private CarsharingStationMobsim station;
		private int travelTime;
		private double distance;
		private CarsharingOfferStatus status;
		private int time;
		public EgressOffer(int depTime, CarsharingStationMobsim eStation, int eDur, double eDist, CarsharingOfferStatus flag) {
			this.station = eStation;
			this.travelTime = eDur;
			this.distance = eDist;
			this.time = depTime;
			this.status = flag;
		}
		EgressOffer(EgressOffer e) {
			this.station = e.station;
			this.travelTime = e.travelTime;
			this.distance = e.distance;
			this.time = e.time;
			this.status = e.status;
		}
		public CarsharingStationMobsim getStation() { return station; }
		public int getTravelTime() { return travelTime; }
		public double getDistance() { return distance; }
		public CarsharingOfferStatus getStatus() { return status; }
		public int getTime() { return this.time; }
	}
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class DriveOffer {
		private int time;
		private int travelTime = 0;
		private int rentalTime = 0;
		private double distance = 0;
		private int offset = 0;
		private List<? extends PlanElement> route = null;
		private int nVEH;
		public DriveOffer(int depTime, int nVEH, RouteData rd){
			this.time = depTime;
			this.nVEH = nVEH;
			if(rd != null) {
				this.route = rd.path; 
				this.distance = rd.distance;
				this.travelTime = rd.time;
				this.offset = rd.offset;
				this.rentalTime = rd.time + 2*rd.offset;
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
		public int getRentalTime() { return this.rentalTime; }
		public int getTravelTime() { return this.travelTime; }
		public double getDistance() { return distance; }
		public List<? extends PlanElement> getRoute() { return this.route; }
		public int getTime() { return this.time; }
		public int getOffset() { return this.offset; }
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
	public boolean hasValidAccess() { return this.access.status.status; }
	public boolean hasValidEgress() { return this.egress.status.status; }
	public CarsharingAgent getAgent() { return this.agent; }
	public double getCost() { return this.cost; }
	public int getDepartureTime() { return this.access.time; }
	public int getArrivalTime() { return this.egress.time + this.egress.travelTime; }
	public int getAccessTime() { return this.access.time + this.access.travelTime; }
	public int getEgressTime() { return this.egress.time; }
	public boolean isValid() { return this.access.status.status && this.egress.status.status; }
	
	
	public void setCost(double c) { this.cost = c; }
	
	// ************
	private final CarsharingAgent agent;
	private final CarsharingDemand demand;
	private final AccessOffer access;
	private final EgressOffer egress;
	private final DriveOffer drive;
	private double cost;
	private int offset;
	
	

	
	public static final CarsharingOfferStatus FAILURE_NODEPARTURESTATION = new CarsharingOfferStatus(false, "no_departure_station");
	public static final CarsharingOfferStatus FAILURE_NOARRIVALSTATION = new CarsharingOfferStatus(false, "no_arrival_station");
	public static final CarsharingOfferStatus FAILURE_NODEPARTUREAVAILABILITY = new CarsharingOfferStatus(false, "no_departure_availability");
	public static final CarsharingOfferStatus FAILURE_NOCHARGEDVEHICLE = new CarsharingOfferStatus(false, "no_charged_vehicle");
	public static final CarsharingOfferStatus FAILURE_NOARRIVALAVAILABILITY = new CarsharingOfferStatus(false, "no_arrival_availability");
	public static final CarsharingOfferStatus FAILURE_FLOATINGLIMIT = new CarsharingOfferStatus(false, "floating_limit");
	
	public static final CarsharingOfferStatus SUCCESS_STANDARDOFFER = new CarsharingOfferStatus(true, "standard_offer");
	public static final CarsharingOfferStatus SUCCESS_FREEFLOATINGOFFER = new CarsharingOfferStatus(true, "freefloating_offer");
	public static final CarsharingOfferStatus OPERATOR_RELOCATION = new CarsharingOfferStatus(true, "operator_relocation");
	public static final CarsharingOfferStatus USER_RELOCATION = new CarsharingOfferStatus(true, "user_relocation");
	public static final CarsharingOfferStatus DUMMY = new CarsharingOfferStatus(true, "dummy");
	
	public static class CarsharingOfferStatus {
		boolean status;
		String flag;
		CarsharingOfferStatus(boolean s, String f) {
			this.status = s;
			this.flag = f;
		}
		public boolean isValid() {
			return status;
		}
	}
		
}
