package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.utils.CarsharingUtils;

/**
 * 
 * @author haitam
 *
 */
public class CarsharingDemand {
	
	private CarsharingAgent person;
	private Activity origin;
	private Activity destination;
	
	private int departureTime;
	private int arrivalTime;
	
	private int numberOfVehicles;
	private Leg carsharingLeg;
	
	private int planindex;
	private int tripindex;
	
	public CarsharingDemand(Leg carsharingLeg, CarsharingAgent person, Activity origin, Activity destination, int numberOfVehicles, int planindex, int tripindex) {
		this(carsharingLeg, person, origin, destination, numberOfVehicles);
		this.tripindex = tripindex;
		this.planindex = planindex;
	}
	
	public CarsharingDemand(Leg carsharingLeg, CarsharingAgent person, Activity origin, Activity destination, int numberOfVehicles) {
		
		this.person = person;
		this.origin = origin;
		this.destination = destination;
		
		double deptime = this.origin.getEndTime();
		if(CarsharingUtils.isNaNorInfinit(deptime)) {
			deptime = (int) this.origin.getStartTime();
			if(CarsharingUtils.isNaNorInfinit(deptime)) {
				throw new RuntimeException("Demand: origin activity with undefined start time & end time");
			} 
			if(!CarsharingUtils.isNaNorInfinit(this.origin.getMaximumDuration())) {
				deptime += this.origin.getMaximumDuration();
			}
		}
		this.departureTime = (int) deptime;
		
		double arrtime = (int) this.destination.getStartTime();
		if(CarsharingUtils.isNaNorInfinit(arrtime)) {
			arrtime = (int) this.destination.getEndTime();
			if(CarsharingUtils.isNaNorInfinit(arrtime)) {
				throw new RuntimeException("Demand: destination activity with undefined start time & end time");
			}
			if(!CarsharingUtils.isNaNorInfinit(this.origin.getMaximumDuration())) {
				arrtime -= this.origin.getMaximumDuration();
			}
		}
		this.arrivalTime = (int) arrtime;
		
		this.numberOfVehicles = numberOfVehicles;
		this.carsharingLeg = carsharingLeg;
	}

	public Leg getID() { return carsharingLeg; }
	public CarsharingAgent getAgent() { return this.person; }		
	public Activity getOrigin() { return this.origin; }
	public Activity getDestination() { return this.destination; }
	public int getRawDepartureTime() { return this.departureTime; }
	public int getRawArrivalTime() { return this.arrivalTime; }
	public int getNbrOfVeh() { return numberOfVehicles; }
	public int getTripIndex() { return this.tripindex; }
	public int getPlanIndex() { return this.planindex; }
	
	public static CarsharingDemand getInstance(CarsharingCustomerMobsim user, Leg carsharingLegID, Plan currentPlan) {
		if(!CarsharingUtils.isUnRoutedCarsharingLeg(carsharingLegID)) {
			return null;
		}
		int unroutedIndex = currentPlan.getPlanElements().indexOf(carsharingLegID);
		Activity accessActivity = (Activity)currentPlan.getPlanElements().get(unroutedIndex - 1);
		Activity egressActivity = (Activity)currentPlan.getPlanElements().get(unroutedIndex + 1);
		CarsharingDemand demand = new CarsharingDemand(carsharingLegID, user, accessActivity, egressActivity, 1);
		return demand;
	}
	
}
