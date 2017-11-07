package org.matsim.haitam.api.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;

import com.google.inject.Inject;

public class CarsharingDirectRouterModule extends CarsharingDefaultRouterModule {

	@Inject
	public CarsharingDirectRouterModule(Scenario scenario, CarsharingManager manager) {
		super(scenario, manager, CarsharingPlanModeCst.directTrip);
	}

}
