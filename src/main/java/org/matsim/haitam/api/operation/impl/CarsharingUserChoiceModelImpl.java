package org.matsim.haitam.api.operation.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.haitam.api.carsharing.core.CarsharingBookingRecord;
import org.matsim.haitam.api.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.operation.model.CarsharingUserChoiceModel;
import org.matsim.haitam.api.utils.CarsharingUtils;

public class CarsharingUserChoiceModelImpl implements CarsharingUserChoiceModel {
	
	public static double PROB = 0;
	protected final Map<PlanElement, CarsharingDemand> demandMap;
	protected final Map<PlanElement, Integer> csIndex;
	protected CarsharingCustomerMobsim user;
	protected int index_trip = 0;
	
	public CarsharingUserChoiceModelImpl() {
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
		for(CarsharingOffer o : offers) {
			if(o.hasValidAccess() && o.hasValidEgress()) {
				if(minCostOffer == null || minCostOffer.getCost() > o.getCost())
					minCostOffer = o;
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
