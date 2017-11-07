package org.matsim.haitam.api.operation.model;

import org.matsim.api.core.v01.population.Person;
import org.matsim.haitam.api.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingMembershipModel extends CarsharingOperationModel {

	// Check in a person as a customer and potential user of the car sharing service
	CarsharingCustomerMobsim checkin(Person p, CarsharingUserChoiceModel decision);
	
}
