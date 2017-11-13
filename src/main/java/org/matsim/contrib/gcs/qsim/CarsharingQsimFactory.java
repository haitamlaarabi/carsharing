package org.matsim.contrib.gcs.qsim;

import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.operation.model.CarsharingBatteryModel;
import org.matsim.contrib.gcs.operation.model.CarsharingEnergyConsumptionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingMembershipModel;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerDistributionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerSourceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class CarsharingQsimFactory implements Provider<Netsim> {
	
	// MATSIM
	@Inject private Scenario sc;
	@Inject private Controler controller;
	@Inject private EventsManager eventsManager;
	@Inject private Provider<TripRouter> tripRouterProvider;	
	
	// CARSHARING
	@Inject private CarsharingManager manager;
	
	@Inject private CarsharingMobsimHandle monitoringEngine;
	
	
	
	@Override
	public Netsim get() {
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. "
					+ "Please add the module 'qsim' to your config file.");
		}
		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		qSim.addMobsimEngine(new TeleportationEngine(sc, eventsManager));
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		if (sc.getConfig().transit().isUseTransit()) {
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		}
		
		
		AgentFactory agentFactory = null;
		CarsharingConfigGroup csconfig = (CarsharingConfigGroup) sc.getConfig().getModules().get(CarsharingConfigGroup.GROUP_NAME);
		if(csconfig != null && csconfig.isActivated()) {
			// CARSHARING PART
			TripRouter tripRouter = this.tripRouterProvider.get();
			monitoringEngine.qSim = qSim;
			monitoringEngine.iteration = controller.getIterationNumber();
			monitoringEngine.sc = sc;
			qSim.addMobsimEngine(monitoringEngine);
			qSim.addDepartureHandler(monitoringEngine);
			agentFactory = new CarsharingAgentFactory(qSim, tripRouter, manager);
			qSim.addAgentSource(new CarsharingAgentSource(agentFactory, qSim, manager));
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		
		qSim.addAgentSource(new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim));	
		
		QNetsimEngineModule.configure(qSim);
		
		return qSim;
	}
	

	
}
