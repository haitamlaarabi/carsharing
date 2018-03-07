package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.api.core.v01.population.Person;

public interface CarsharingAgent {

	String getId();
	Person getPerson();
	void reset(int iteration);
	
}
