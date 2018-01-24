package org.matsim.contrib.gcs.operation.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;
import org.matsim.contrib.gcs.utils.CarsharingUtils;

public class CarsharingUserChoiceModelImpl2 implements CarsharingUserChoiceModel {
	
	public static double PROB = 0;
	protected final Map<PlanElement, CarsharingDemand> demandMap;
	protected final Map<PlanElement, Integer> csIndex;
	protected CarsharingCustomerMobsim user;
	protected int index_trip = 0;
	
	public CarsharingUserChoiceModelImpl2() {
		this.demandMap = new HashMap<PlanElement, CarsharingDemand>();
		this.csIndex = new HashMap<PlanElement, Integer>();
	}
	
	@Override
	public void bindTo(CarsharingCustomerMobsim user) {
		this.user = user;
	}
	
	@Override
	public CarsharingOffer selectOffer(Collection<CarsharingOffer> offers) {
		CarsharingOffer minCostOffer = null;
		double minTotCost = Double.MAX_VALUE;
		for(CarsharingOffer o : offers) {
			if(o.hasValidAccess() && o.hasValidEgress()) {
				final double trip_cost = o.getCost() * (o.getDrive().getRentalTime());
				if(minCostOffer == null || minTotCost > trip_cost)
					minCostOffer = o;
					minTotCost = trip_cost;
			} 
		}
		return minCostOffer;
	}	

	@Override
	public boolean acceptBooking(CarsharingBookingRecord booking, double time) {
		if(booking != null) {
			user.setStatus(null, booking);
			return true;
		} 
		return false;
	}

	@Override
	public CarsharingDemand getOrConstructDemand(Leg carsharingLegID, Plan currentPlan) {
		if(this.demandMap.containsKey(carsharingLegID)) {
			return this.demandMap.get(carsharingLegID);
		}
		if(!CarsharingUtils.isUnRoutedCarsharingLeg(carsharingLegID)) {
			return null;
		}

		if(!this.csIndex.containsKey(carsharingLegID)) {
			index_trip++;
			this.csIndex.put(carsharingLegID, index_trip);
		}
		int unroutedIndex = currentPlan.getPlanElements().indexOf(carsharingLegID);
		Activity accessActivity = (Activity)currentPlan.getPlanElements().get(unroutedIndex - 1);
		Activity egressActivity = (Activity)currentPlan.getPlanElements().get(unroutedIndex + 1);
		CarsharingDemand demand = new CarsharingDemand(carsharingLegID, user, accessActivity, egressActivity, 1, unroutedIndex, this.csIndex.get(carsharingLegID));
		this.demandMap.put(demand.getID(), demand);
		return demand;
	}
}
