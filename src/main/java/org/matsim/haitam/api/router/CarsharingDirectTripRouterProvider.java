package org.matsim.haitam.api.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.RoutingModule;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;

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
