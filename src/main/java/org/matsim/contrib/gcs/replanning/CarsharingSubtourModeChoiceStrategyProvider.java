package org.matsim.contrib.gcs.replanning;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.replanning.algos.CarsharingSubTourPermissableModesCalculator;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;

public class CarsharingSubtourModeChoiceStrategyProvider implements Provider<PlanStrategy> {

	public static final String STRATEGY_TYPE = "CarsharingSubtourModeChoiceStrategy";
	private Provider<TripRouter> tripRouterProvider;
	private final Scenario scenario;
	private final CarsharingManager manager;
	
	@Inject
	public CarsharingSubtourModeChoiceStrategyProvider(
			final Scenario scenario, 
			Provider<TripRouter> tripRouterProvider, 
			CarsharingManager manager) {
		
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
		this.manager = manager;
	}

	
	@Override
	public PlanStrategy get() {
		CarsharingSubTourPermissableModesCalculator cpmc = 
				new CarsharingSubTourPermissableModesCalculator(scenario, manager, scenario.getConfig().subtourModeChoice().getModes());
		SubtourModeChoice smc = new SubtourModeChoice(tripRouterProvider, scenario.getConfig().global(), scenario.getConfig().subtourModeChoice());
		smc.setPermissibleModesCalculator(cpmc);
		//
		
		PlanStrategyImpl strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );	
		strategy.addStrategyModule(smc );
		strategy.addStrategyModule( new ReRoute(scenario, tripRouterProvider) );
		return strategy;
	}

}
