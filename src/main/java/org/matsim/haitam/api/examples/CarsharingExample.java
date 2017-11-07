package org.matsim.haitam.api.examples;

import java.io.File;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.haitam.api.carsharing.CarsharingScenario;
import org.matsim.haitam.api.carsharing.CarsharingScenarioReader;
import org.matsim.haitam.api.carsharing.core.CarsharingStation;
import org.matsim.haitam.api.config.CarsharingInstaller;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;

public class CarsharingExample {
	
	protected String rootDir = "examples";

	public static void main(String[] args) {	
		new CarsharingExample().run("CarsharingExample", "carsharing.xml");
	}
	
	protected void modulesToInstance(CarsharingInstaller installer) {
		installer.carsharing.getConfig().setCarsharingScenarioInputFile(rootDir + "/data/carsharing.xml");
		installer.carsharing.getConfig().setActivateModule(true);
		installer.carsharing.getConfig().setRentalRatePerMin(-0.0);
		installer.carsharing.getConfig().setSearchDistance(500.0);
	}
	
	
	public void run(final String exampleDir, final String carsharingfile) {
		if(!new File(rootDir + "/" + exampleDir ).exists()) {
			new File(rootDir + "/" + exampleDir ).mkdir();
		}
		final Config config = ConfigUtils.loadConfig(rootDir + "/data/0.config.xml");
		config.controler().setOutputDirectory(rootDir + "/" + exampleDir + "/output");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		
		String logdir =  rootDir + "/" + exampleDir + "/output/log";
		CarsharingInstaller installer = new CarsharingInstaller(scenario, controler, logdir){
			@Override
			public void installOrOverrideModules() {
				modulesToInstance(this);
			}

			@Override
			public void init() {
				
			}
		};
		
		controler.addOverridingModule(installer);
		new CarsharingScenarioReader(installer.getCarsharingScenario(), scenario).readXml( rootDir + "/data/" +  carsharingfile);
		generateCarsharingDemand(scenario, installer.getCarsharingScenario(), 100);
		new FacilitiesWriter(scenario.getActivityFacilities()).writeV1( rootDir + "/data/" + carsharingfile + "_facilities.xml");
		new PopulationWriter(scenario.getPopulation()).writeV5( rootDir + "/" + "/data/" + carsharingfile + "_plans.xml");
		controler.run();
	}
	

	// Default behavior, customers will be living and working within a radius of 500m
	protected void generateCarsharingDemand(Scenario scenario, CarsharingScenario carsharing, int demandSize) {		
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		int facility_counter = 0;
		for(int i = 0 ; i < demandSize ; i++) {
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("c"+i));
			Plan plan = PopulationUtils.createPlan(p);

			// home
			CarsharingStation start_station = getRandomStation(carsharing, null);
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Id<Link> hlink = NetworkUtils.getNearestLink(scenario.getNetwork(), h).getId();
			Activity home1 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", h);
			home1.setEndTime(8 * 3600);
			home1.setLinkId(hlink);
			ActivityFacility hfacility = factory.createActivityFacility(Id.create((facility_counter++)+"h", ActivityFacility.class), h);
			home1.setFacilityId(hfacility.getId());
			
			// leg
			Leg leg1 = PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// work
			CarsharingStation dest_station = getRandomStation(carsharing, start_station);
			Coord w = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity work = PopulationUtils.createAndAddActivityFromCoord(plan, "w", w);
			Id<Link> wlink = NetworkUtils.getNearestLink(scenario.getNetwork(), w).getId();
			work.setLinkId(wlink);
			work.setEndTime(17 * 3600);
			ActivityFacility wfacility = factory.createActivityFacility(Id.create((facility_counter++)+"w", ActivityFacility.class), w);
			work.setFacilityId(wfacility.getId());
			
			// leg
			Leg leg2 = PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home
			Activity home2 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", h);
			home2.setLinkId(hlink);
			home2.setFacilityId(hfacility.getId());
			

			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			scenario.getActivityFacilities().addActivityFacility(hfacility);
			scenario.getActivityFacilities().addActivityFacility(wfacility);
		}
	}
	
	protected static CarsharingStation getRandomStation(CarsharingScenario carsharing, CarsharingStation toexclude) {
		ArrayList<CarsharingStation> stations = new ArrayList<CarsharingStation>();
		for(CarsharingStation s : carsharing.getStations().values()) {
			if(s != toexclude) {
				stations.add(s);
			}
		}
		return stations.get(MatsimRandom.getRandom().nextInt(stations.size()));
	}
	
	protected static Coord getRandomCoordInDisk(Coord center, double radius) {
		Double a = MatsimRandom.getRandom().nextDouble();
		Double b = MatsimRandom.getRandom().nextDouble();
		if(b < a) {
			double c = b;
			b = a;
			a = c;
		}
		return new Coord(center.getX() + b*radius*Math.cos(2*Math.PI*a/b), center.getY() + b*radius*Math.sin(2*Math.PI*a/b));
	}
	
	
}
