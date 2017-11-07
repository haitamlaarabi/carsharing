package org.matsim.haitam.api.replanning;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.replanning.algos.ChangeToCarsharingDirectTripLegMode;

import com.google.inject.Inject;


public class RandomChangeTripModePlanStrategyProvider implements Provider<PlanStrategy> {
	
	public static final String STRATEGY_TYPE = "RandomChangeTripModePlanStrategy";
	private Provider<TripRouter> tripRouterProvider;
	private final Scenario scenario;
	private final CarsharingManager manager;
	
	@Inject
	public RandomChangeTripModePlanStrategyProvider(
			final Scenario scenario, 
			Provider<TripRouter> tripRouterProvider, 
			CarsharingManager manager) {
		
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
		this.manager = manager;
	}
	@Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );
		strategy.addStrategyModule(new ChangeToCarsharingDirectTripLegMode(scenario, tripRouterProvider, this.manager) );
		strategy.addStrategyModule( new ReRoute(scenario, tripRouterProvider) );
		return strategy;
	}
	
}
