package org.matsim.contrib.gcs.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CarsharingDirectTripRouterProvider implements Provider<RoutingModule> {

	@Inject Scenario scenario;
	@Inject CarsharingManager manager;
	
	@Override
	public RoutingModule get() {
		return new CarsharingNearestStationRouterModule(scenario, manager, CarsharingPlanModeCst.directTrip);
	}

}
