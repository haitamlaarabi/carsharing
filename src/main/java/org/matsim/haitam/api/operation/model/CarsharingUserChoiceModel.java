package org.matsim.haitam.api.operation.model;

import java.util.Collection;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.haitam.api.carsharing.core.CarsharingBookingRecord;
import org.matsim.haitam.api.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingUserChoiceModel extends CarsharingOperationModel {
	
	// Methods
	CarsharingOffer selectOffer(Collection<CarsharingOffer> offers);
	CarsharingDemand getOrConstructDemand(Leg carsharingLegID, Plan currentPlan);
	boolean acceptBooking(CarsharingBookingRecord booking, double time);
	//boolean askForAnotherOffer();
	void bindTo(CarsharingCustomerMobsim user);

}
