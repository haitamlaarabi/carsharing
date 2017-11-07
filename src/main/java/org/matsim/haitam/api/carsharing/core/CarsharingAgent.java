package org.matsim.haitam.api.carsharing.core;

import org.matsim.api.core.v01.population.Person;

public interface CarsharingAgent {

	String getId();
	Person getPerson();
	void reset(int iteration);
	
}
