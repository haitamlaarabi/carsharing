package org.matsim.contrib.gcs.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.analysis.LegHistogramModule;
import org.matsim.analysis.LegTimesModule;
import org.matsim.analysis.ScoreStatsModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.gcs.config.CarsharingInstaller;
import org.matsim.contrib.gcs.config.CarsharingRelocationParams;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public class CarsharingInstallerDefault extends CarsharingInstaller {

	public CarsharingInstallerDefault(Scenario scenario, Controler controler, String rootdir) {
		super(scenario, controler, rootdir);
		manager.getConfig().setSearchDistance(500.0);
		manager.getConfig().setCarsharingScenarioInputFile(rootdir+"/carsharing-scenario.xml");
		manager.getConfig().setInteractionOffset(3*60);
		manager.getConfig().setActivateModule(true);
		
		manager.getConfig().setRentalRatePerMin(-1.0);
		manager.getConfig().setConstantRate(0.0);
		manager.getConfig().setLogFrequency(1);
		
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
		
		CarsharingRelocationParams rparams = manager.getConfig().getRelocation();
		rparams.setActivate_from_iter(1);
		rparams.setDeactivate_after_iter(0);
		rparams.setMaxtrain(7);
		rparams.setPerfomance_file(rootdir+"/log/relocation-perf.log");
		rparams.setTask_output_file(rootdir+"/log/relocation-tasks");
		rparams.setTrace_output_file(rootdir+"/log/relocation-traces");
		
		VehicleType privatecar = new VehicleTypeImpl(Id.create("car",VehicleType.class));
		privatecar.setDescription("private car");
		privatecar.setMaximumVelocity(12.5);
		privatecar.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(privatecar);
		VehicleType cscar = new VehicleTypeImpl(Id.create("cs_drive",VehicleType.class));
		cscar.setDescription("carsharing");
		cscar.setMaximumVelocity(12.5);
		cscar.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(cscar);
		
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().transit().setTransitModes(new HashSet<String>(Arrays.asList(TransportMode.pt, CarsharingRouterUtils.cs_pt)));
		scenario.getConfig().transitRouter().setAdditionalTransferTime(2*60.0);
		scenario.getConfig().transitRouter().setExtensionRadius(200.0);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(100.0);
		scenario.getConfig().transitRouter().setSearchRadius(1000.0);
		ArrayList<String> subModes = new ArrayList<String>(Arrays.asList(scenario.getConfig().subtourModeChoice().getModes()));
		subModes.add(TransportMode.pt);
		subModes.add(CarsharingRouterUtils.cs_pt);
		scenario.getConfig().subtourModeChoice().setModes(subModes.toArray(new String[0]));
		
		scenario.getConfig().plansCalcRoute().addModeRoutingParams(CarsharingUtils.createModeRouting(TransportMode.walk, 1.3, 3.0)); // 3.0 km/h
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.walk, -0.6, 0, 11.29)); // 0 euros
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.pt, -0.3, 0, 6.14)); // 0.01 euros
		scenario.getConfig().planCalcScore().addModeParams(CarsharingUtils.createModeParam(TransportMode.transit_walk, -0.6, 0, 11.29)); // 0 euros
		
		Network carNetwork = NetworkUtils.createNetwork();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		filter.filter(carNetwork, modes);
		for(ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Link link = NetworkUtils.getNearestLink(carNetwork, facility.getCoord());
			((ActivityFacilityImpl)facility).setLinkId(link.getId());
		}
		
		controler.addOverridingModule(new LegHistogramModule());	
		controler.addOverridingModule(new LegTimesModule());
		controler.addOverridingModule(new ScoreStatsModule());	
	}

	@Override
	public void init() {
		super.init();
		manager.stop_deployment_at_iteration(0);
	}
	
	@Override
	public void installOrOverrideModules() {
		bind(TravelTime.class).to(TravelTimeCollector.class);
		addEventHandlerBinding().to(TravelTimeCollector.class);
		addMobsimListenerBinding().to(TravelTimeCollector.class);
	}

}
