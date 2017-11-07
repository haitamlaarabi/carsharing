package org.matsim.haitam.api.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.facilities.Facility;
import org.matsim.haitam.api.carsharing.CarsharingManager;

import com.google.inject.Inject;

public class CarsharingDefaultRouterModule implements RoutingModule {

	final Scenario scenario;
	final CarsharingManager manager;
	final StageActivityTypes stageActivityTypes;
	final String cssMode;
	
	@Inject
	public CarsharingDefaultRouterModule(Scenario scenario, CarsharingManager manager, String cssMode) {
		this.scenario = scenario;
		this.manager = manager;
		this.cssMode = cssMode;
		this.stageActivityTypes =  new StageActivityTypesImpl(CarsharingRouterModeCst.ACTIVITY_TYPE_NAME);
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		Leg driveLeg = PopulationUtils.createLeg(this.cssMode);
		driveLeg.setRoute(RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId()));
		trip.add(driveLeg);
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return this.stageActivityTypes;
	}

}
