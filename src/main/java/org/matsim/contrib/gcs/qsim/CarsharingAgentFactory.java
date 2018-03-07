package org.matsim.contrib.gcs.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

public class CarsharingAgentFactory implements AgentFactory {
	
	private final Netsim simulation;
	private final CarsharingManager manager;
	private final TripRouter tripRouter;
	
	public CarsharingAgentFactory(final Netsim simulation, TripRouter tripRouter, final CarsharingManager manager) {
		this.simulation = simulation;
		this.manager = manager;
		this.tripRouter = tripRouter;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		MobsimDriverAgent agent = new CarsharingAgentBehaviour(
										PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), 
										this.simulation,
										this.tripRouter,
										this.manager); 
		return agent;
	}
}
