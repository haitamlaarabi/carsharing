package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
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
	
	private double departureTime;
	private double arrivalTime;
	
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
		
		this.departureTime = this.origin.getEndTime();
		if(CarsharingUtils.isNaNorInfinit(this.departureTime)) {
			this.departureTime = this.origin.getStartTime();
			if(CarsharingUtils.isNaNorInfinit(this.departureTime)) {
				throw new RuntimeException("Demand: origin activity with undefined start time & end time");
			} 
			if(!CarsharingUtils.isNaNorInfinit(this.origin.getMaximumDuration())) {
				this.departureTime += this.origin.getMaximumDuration();
			}
		}
		
		this.arrivalTime = this.destination.getStartTime();
		if(CarsharingUtils.isNaNorInfinit(this.arrivalTime)) {
			this.arrivalTime = this.destination.getEndTime();
			if(CarsharingUtils.isNaNorInfinit(this.arrivalTime)) {
				throw new RuntimeException("Demand: destination activity with undefined start time & end time");
			}
			if(!CarsharingUtils.isNaNorInfinit(this.origin.getMaximumDuration())) {
				this.arrivalTime -= this.origin.getMaximumDuration();
			}
		}
		
		this.numberOfVehicles = numberOfVehicles;
		this.carsharingLeg = carsharingLeg;
	}

	public Leg getID() { return carsharingLeg; }
	public CarsharingAgent getAgent() { return this.person; }		
	public Activity getOrigin() { return this.origin; }
	public Activity getDestination() { return this.destination; }
	public double getRawDepartureTime() { return this.departureTime; }
	public double getRawArrivalTime() { return this.arrivalTime; }
	public int getNbrOfVeh() { return numberOfVehicles; }
	public int getTripIndex() { return this.tripindex; }
	public int getPlanIndex() { return this.planindex; }
	
}
