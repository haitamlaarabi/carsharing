package org.matsim.contrib.gcs.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;

import com.google.inject.Inject;

public class CarsharingDirectRouterModule extends CarsharingDefaultRouterModule {

	@Inject
	public CarsharingDirectRouterModule(Scenario scenario, CarsharingManager manager) {
		super(scenario, manager, CarsharingPlanModeCst.directTrip);
	}

}
