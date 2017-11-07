package org.matsim.haitam.api.control;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.CarsharingPreprocessedData;
import org.matsim.haitam.api.carsharing.CarsharingScenarioWriter;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import com.google.inject.Inject;

public class ControllerListener
		implements StartupListener, IterationEndsListener, IterationStartsListener, ScoringListener {

	// MATSIM
	@Inject private Scenario sc;
	@Inject private CarsharingManager m;
	@Inject private CarsharingPreprocessedData data;
	
	AgentEventsListener agentEventsHandler;
	CarsharingEventsListener carsharingEventsHandler;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.m.finalizeAndwriteCurrentIterationLogs(event.getIteration());
		this.data.update(event.getIteration(), m);
		new CarsharingScenarioWriter(this.m).writeXml(this.m.getConfig().getLogDir() + "/carsharing_scenario_it"+event.getIteration()+".xml");
		event.getServices().getEvents().removeHandler(this.agentEventsHandler);
		event.getServices().getEvents().removeHandler(this.carsharingEventsHandler);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		new File(this.m.getConfig().getLogDir()).mkdirs();
		this.m.setUp(event.getServices(), null);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.agentEventsHandler = new AgentEventsListener(sc, this.m);
		this.carsharingEventsHandler = new CarsharingEventsListener(this.m);
		event.getServices().getEvents().addHandler(this.agentEventsHandler);
		event.getServices().getEvents().addHandler(this.carsharingEventsHandler);
		this.m.reset(event.getIteration());
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		
	}
}
