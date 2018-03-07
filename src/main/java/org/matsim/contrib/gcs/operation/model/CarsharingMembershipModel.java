package org.matsim.contrib.gcs.operation.model;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingMembershipModel extends CarsharingOperationModel {

	// Check in a person as a customer and potential user of the car sharing service
	CarsharingCustomerMobsim checkin(Person p, CarsharingUserChoiceModel decision);
	
}
