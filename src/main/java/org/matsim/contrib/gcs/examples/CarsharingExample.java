package org.matsim.contrib.gcs.examples;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogramModule;
import org.matsim.analysis.LegTimesModule;
import org.matsim.analysis.ScoreStatsModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.gcs.qsim.CarsharingMobsimHandle;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import com.google.inject.Inject;

public class CarsharingExample {
	
	private static Logger logger = Logger.getLogger(CarsharingExample.class);
	static String rootDir = "examples/"+CarsharingExample.class.getSimpleName();

	public static void main(String[] args) {
		
		final Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(rootDir + "/output");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.transit().setTransitScheduleFile(rootDir + "/transit-schedule.xml");
		config.transit().setVehiclesFile(rootDir + "/transit-vehicles.xml");
		config.network().setInputFile(rootDir + "/multimodalnetwork.xml");
		config.planCalcScore().addActivityParams(CarsharingUtils.createActivityParam("h", CarsharingUtils.toSecond(0,0,0), CarsharingUtils.toSecond(23,59,59),  CarsharingUtils.toSecond(11,0,0)));
		config.planCalcScore().addActivityParams(CarsharingUtils.createActivityParam("w", CarsharingUtils.toSecond(7,0,0), CarsharingUtils.toSecond(21,0,0), CarsharingUtils.toSecond(8,0,0)));

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		
		CarsharingInstallerDefault installer = new CarsharingInstallerDefault(scenario, controler, rootDir + "/log") {
			@Override
			public void init() {
				manager.getConfig().setCarsharingScenarioInputFile(rootDir + "/carsharing-scenario.xml");
				//manager.getConfig().setCarsharingScenarioInputFile(rootDir + "/user-reloc-scenario.xml");
				super.init();
			}
			@Override
			public void installOrOverrideModules() {
				super.installOrOverrideModules();
				
				/// ********
				bindCarsharingMobsimMonitoring(TestingTravelTimeCollector.class);
				
				bind(TravelTime.class).to(TravelTimeCollector.class);
				addEventHandlerBinding().to(TravelTimeCollector.class);
				bindNetworkTravelTime().to(TravelTimeCollector.class);
				addMobsimListenerBinding().to(TravelTimeCollector.class);
				// *********
				
			}
		};
		installer.init();
		
		CarsharingDemandGenerator.dummy(scenario, installer.getCarsharingScenario(), 100);
		//CarsharingDemandGenerator.scenario_user_relocation(scenario, installer.getCarsharingScenario());
		
		controler.addOverridingModule(installer);
		controler.addOverridingModule(new LegHistogramModule());	
		controler.addOverridingModule(new LegTimesModule());
		controler.addOverridingModule(new ScoreStatsModule());
		controler.run();
		logger.info("END :)");
	}
	
	public static class TestingTravelTimeCollector extends CarsharingMobsimHandle {
		@Inject private TravelTime tt;
		HashMap<Link, Double> testing = new HashMap<Link, Double>();
		@Override
		protected void execute(double time) {
			TravelTimeCollector ttc = (TravelTimeCollector) tt;
			for(Link l : sc.getNetwork().getLinks().values()) {
				double t = ttc.getLinkTravelTime(l, time, null, null);
				if(!testing.containsKey(l)) {
					testing.put(l,  t);
				} else {
					if (testing.get(l) != t) {
						System.out.println("working");
					}
				}
			}
		}
	}
	

}
