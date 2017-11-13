package org.matsim.contrib.gcs.carsharing.core;

import java.util.List;

public interface CarsharingOperators extends GeoContainer<CarsharingOperatorMobsim> {
	
	List<CarsharingOperatorMobsim> availableSet();
	
}
