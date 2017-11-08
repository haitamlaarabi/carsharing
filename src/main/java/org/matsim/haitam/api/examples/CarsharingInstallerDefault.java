package org.matsim.haitam.api.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.haitam.api.config.CarsharingInstaller;
import org.matsim.haitam.api.utils.CarsharingUtils;

public class CarsharingInstallerDefault extends CarsharingInstaller {

	public CarsharingInstallerDefault(Scenario scenario, Controler controler, String logdir) {
		super(scenario, controler, logdir);
		manager.getConfig().setSearchDistance(500.0);
		manager.getConfig().setCarsharingScenarioInputFile(logdir + "/carsharing-scenario.xml");
		manager.getConfig().setInteractionOffset(3*60);
		
		manager.getConfig().setRentalRatePerMin(-1.0);
		manager.getConfig().setConstantRate(0.0);
		
		manager.getConfig().getDriveCalcScore().setMonetaryDistanceRate(0.0);
		manager.getConfig().getDriveCalcScore().setMarginalUtilityOfTraveling(-0.8*60.0+6);
		manager.getConfig().getDriveCalcScore().setMarginalUtilityOfDistance(0.0);
		manager.getConfig().getDriveCalcScore().setConstant(15.47);
		
		manager.getConfig().getAccessWalkCalcScore().setMonetaryDistanceRate(0.0);
		manager.getConfig().getAccessWalkCalcScore().setMarginalUtilityOfTraveling(-0.6*60+6);
		manager.getConfig().getAccessWalkCalcScore().setMarginalUtilityOfDistance(0.0);
		manager.getConfig().getAccessWalkCalcScore().setConstant(11.29);
		
		manager.getConfig().getEgressWalkCalcScore().setMonetaryDistanceRate(0.0);
		manager.getConfig().getEgressWalkCalcScore().setMarginalUtilityOfTraveling(-0.6*60+6);
		manager.getConfig().getEgressWalkCalcScore().setMarginalUtilityOfDistance(0.0);
		manager.getConfig().getEgressWalkCalcScore().setConstant(11.29);
		
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().transit().setTransitModes(new HashSet<String>(Arrays.asList(TransportMode.pt)));
		scenario.getConfig().transitRouter().setAdditionalTransferTime(2*60.0);
		scenario.getConfig().transitRouter().setExtensionRadius(200.0);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(100.0);
		scenario.getConfig().transitRouter().setSearchRadius(1000.0);
		ArrayList<String> subModes = new ArrayList<String>(Arrays.asList(scenario.getConfig().subtourModeChoice().getModes()));
		subModes.add(TransportMode.pt);
		scenario.getConfig().subtourModeChoice().setModes(subModes.toArray(new String[0]));
		
		scenario.getConfig().plansCalcRoute().addModeRoutingParams(CarsharingUtils.createModeRouting(TransportMode.walk, 1.3, 3.0)); // 3.0 km/h
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.walk, -0.6, 0, 11.29)); // 0 euros
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.pt, -0.3, 0, 6.14)); // 0.01 euros
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.transit_walk, -0.6, 0, 11.29)); // 0 euros
	}

	@Override
	public void init() {
		super.init();
		manager.stop_deployment_at_iteration(0);
	}
	
	@Override
	public void installOrOverrideModules() {
		// no additional modules
	}

}
