package org.matsim.contrib.gcs.examples;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

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
		
		CarsharingInstallerDefault installer = new CarsharingInstallerDefault(scenario, controler, rootDir) {
			@Override
			public void init() {
				super.init();
				scenario.getConfig().qsim().setFlowCapFactor(1);
				scenario.getConfig().qsim().setStorageCapFactor(1);
			}
			@Override
			public void installOrOverrideModules() {
				super.installOrOverrideModules();						
			}
		};
		
		//CarsharingDemandGenerator.scenario_user_relocation(scenario, installer.getCarsharingScenario());
	
		installer.init();
		CarsharingDemandGenerator.dummy(scenario, installer.getCarsharingScenario(), 100);
		controler.addOverridingModule(installer);
		controler.run();
		
		logger.info("END :)");
	}

}
