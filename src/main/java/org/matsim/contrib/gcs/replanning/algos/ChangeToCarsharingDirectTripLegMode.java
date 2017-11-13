package org.matsim.contrib.gcs.replanning.algos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * @author haitam
 */
public class ChangeToCarsharingDirectTripLegMode extends AbstractMultithreadedModule {
	
	
	final Provider<TripRouter> tripRouterProvider;
	final ArrayList<String> availableModes = new ArrayList<String>();
	final CarsharingManager manager;

	public ChangeToCarsharingDirectTripLegMode(Scenario scenario, Provider<TripRouter> tripRouterProvider, CarsharingManager manager) {
		super(scenario.getConfig().global().getNumberOfThreads());
		this.tripRouterProvider = tripRouterProvider;
		this.manager = manager;
		if (manager.getConfig().isActivated()) {
			/*this.availableModes.add(CarsharingPlanMode.startTrip);
			this.availableModes.add(CarsharingPlanMode.endTrip);
			this.availableModes.add(CarsharingPlanMode.ongoingTrip);*/
			this.availableModes.add(CarsharingPlanModeCst.directTrip);
		}
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new RandomTripModePlanMutation(
				this.manager, 
				this.availableModes.toArray(new String[0]), 
				MatsimRandom.getRandom(), 
				tripRouterProvider.get().getStageActivityTypes());
	}
	
	
	
	/**
	 * @author haitam
	 * This module allows to randomly mutate a trip mode, in case of a non chaine based mode.
	 * This module is the main source of CarSharing injection into the MobSim
	 */
	public class RandomTripModePlanMutation implements PlanAlgorithm {
		
		protected final String[] possibleModes;
		//private boolean ignoreCarAvailability = true;
		protected final Random rng;
		protected final StageActivityTypes stageActivityTypes;
		protected final CarsharingManager manager;

		public RandomTripModePlanMutation(final CarsharingManager manager, final String[] possibleModes, final Random rng, final StageActivityTypes stageActivityTypes) {
			this.possibleModes = possibleModes.clone();
			this.rng = rng;
			this.stageActivityTypes = stageActivityTypes;
			this.manager = manager;
		}
		
		@Override
		public void run(Plan plan) {
			List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivityTypes);
			if (trips.size() != 0) {
				// GET A RANDOM TRIP FROM THE LIST OF TRIPS IN A PLAN
				int rndIdx = this.rng.nextInt(trips.size());
				
				// IF TRIPS HAS CHAINE BASED MODE, RETURN
				for(Leg leg:trips.get(rndIdx).getLegsOnly()) {
					if (leg.getMode().equals( TransportMode.car ) || 
							leg.getMode().equals( "motorbike" ) ||
							leg.getMode().equals( TransportMode.bike )) {
						return;
					}
				}
				
				//don't change the trips between the same links
				if (!trips.get(rndIdx).getOriginActivity().getLinkId().toString().equals(trips.get(rndIdx).getDestinationActivity().getLinkId().toString())) {
					final Trip trip = trips.get(rndIdx);
					CarsharingCustomerMobsim customer = manager.customers().map().get(plan.getPerson().getId());
					if(customer != null) {
						TripRouter.insertTrip(
								plan,
								trip.getOriginActivity(),
								Collections.singletonList( PopulationUtils.createLeg( CarsharingPlanModeCst.directTrip ) ),
								trip.getDestinationActivity());
					} else {
						TripRouter.insertTrip(plan, trip.getOriginActivity(), trip.getTripElements(), trip.getDestinationActivity());
					}
				} else {
					return;
				}
			}
		}

	}

}
