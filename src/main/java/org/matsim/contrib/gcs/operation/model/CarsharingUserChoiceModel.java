package org.matsim.contrib.gcs.operation.model;

import java.util.Collection;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingUserChoiceModel extends CarsharingOperationModel {
	
	// Methods
	CarsharingOffer selectOffer(Collection<CarsharingOffer> offers);
	CarsharingDemand getOrConstructDemand(Leg carsharingLegID, Plan currentPlan);
	boolean acceptBooking(CarsharingBookingRecord booking, double time);
	//boolean askForAnotherOffer();
	void bindTo(CarsharingCustomerMobsim user);

}
