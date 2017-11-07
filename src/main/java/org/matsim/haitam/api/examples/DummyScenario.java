package org.matsim.haitam.api.examples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.haitam.api.config.CarsharingInstaller;

public class DummyScenario {

	public static void main(String[] args) {
		
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(config);
		final Controler controler = new Controler(scenario);
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory("xxoutputdir");
		config.network().setInputFile("/xxx");
		config.plans().setInputFile("/xxx");
		config.facilities().setInputFile("/xxx");
		
		controler.addOverridingModule(new CarsharingInstaller(scenario, controler, "xxx-logdir"){
			@Override
			public void installOrOverrideModules() {
			}

			@Override
			public void init() {
				carsharing.getConfig().setCarsharingScenarioInputFile("xxx");
				carsharing.getConfig().setActivateModule(true);
				carsharing.getConfig().setRentalRatePerMin(-0.0);
				carsharing.getConfig().setSearchDistance(500.0);
			}
		});

		controler.run();
	}
}
