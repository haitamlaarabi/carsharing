package org.matsim.haitam.api.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.haitam.api.carsharing.CarsharingScenario;
import org.matsim.haitam.api.carsharing.core.CarsharingStation;
import org.matsim.haitam.api.config.CarsharingInstaller;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;

public class CarsharingUserRelocationExample extends CarsharingExample {

	public static void main(String[] args) {
		new CarsharingUserRelocationExample().run("CarsharingUserRelocationExample", "carsharing_scenario1.xml");
	}
	
	@Override
	protected void modulesToInstance(CarsharingInstaller installer) {
		super.modulesToInstance(installer);
	}
	
	@Override
	protected void generateCarsharingDemand(Scenario scenario, CarsharingScenario carsharing, int demandSize) {
		scenario1(scenario, carsharing);
	}
	
	// scenario 1
	// 2 stations, one with 2 vehicles, and other with 0 vehicle
	// numberOfVehicles		station1		station2
	// t0						+2				0
	// t1						-1 (-1) ---->	+2 	
	// t2						+1		<----	-1
	// t3						+1		<----	-1
	// t4						-1		---->	+1
	// t5						+1		<----	-1
	// t6						+1		<----	-1
	void scenario1(Scenario scenario, CarsharingScenario carsharing) {
		
		Id<ActivityFacility> idStation1 = Id.create("S1", ActivityFacility.class);
		Id<ActivityFacility> idStation2 = Id.create("S2", ActivityFacility.class);
		
		{
			// Person 1 
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p1"));
			Plan plan = PopulationUtils.createPlan(p);
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);

			// home
			CarsharingStation start_station = carsharing.getStations().get(idStation1);
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity home1 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp1");
			home1.setEndTime(8 * 3600);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// work
			CarsharingStation dest_station = carsharing.getStations().get(idStation2);
			Coord w = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());	
			Activity work = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "w", w, "wp1");
			work.setEndTime(17 * 3600);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home
			Activity home2 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp1");
			home2.setStartTime(17 * 3600 + 5 * 60);
		}
		
		{
			// Person 2 
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p2"));
			Plan plan = PopulationUtils.createPlan(p);
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			
			// home
			CarsharingStation start_station = carsharing.getStations().get(idStation2);
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity home1 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp2");
			home1.setEndTime(8 * 3600 + 30 * 60);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// work
			CarsharingStation dest_station = carsharing.getStations().get(idStation1);
			Coord w = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());	
			Activity work = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "w", w, "wp2");
			work.setEndTime(16 * 3600 + 30 * 60);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home
			Activity home2 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp2");
			home2.setStartTime(16 * 3600 + 45 * 60);
		}
		
		{
			// Person 3  (basically does same thing as p2)
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p3"));
			Plan plan = PopulationUtils.createPlan(p);
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			
			// home
			CarsharingStation start_station = carsharing.getStations().get(idStation2);
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity home1 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp3");
			home1.setEndTime(8 * 3600 + 30 * 60);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// work
			CarsharingStation dest_station = carsharing.getStations().get(idStation1);
			Coord w = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());	
			Activity work = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "w", w, "wp3");
			work.setEndTime(16 * 3600 + 30 * 60);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home
			Activity home2 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp3");
			home2.setStartTime(16 * 3600 + 45 * 60);
		}
		
		{
			// Person 4 **t3 (basically does same thing as p2)
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("p4"));
			Plan plan = PopulationUtils.createPlan(p);
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			
			// home
			CarsharingStation start_station = carsharing.getStations().get(idStation2);
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity home1 = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "h", h, "hp4");
			home1.setEndTime(17 * 3600);
			
			// leg
			PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// work
			CarsharingStation dest_station = carsharing.getStations().get(idStation1);
			Coord w = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());	
			Activity work = createAndGetActivity(scenario, carsharing.getCarNetwork(), plan, "w", w, "wp4");
			work.setEndTime(17 * 3600 + 15 * 60);
		}
		
	}
	
	
	Activity createAndGetActivity(Scenario scenario, Network carNetwork, Plan plan, String type, Coord coord, String id) {
		Id<ActivityFacility> idaf = Id.create(id, ActivityFacility.class);
		ActivityFacility xfacility = scenario.getActivityFacilities().getFacilities().get(idaf);
		if(xfacility == null) {
			xfacility = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(id, ActivityFacility.class), coord);
			scenario.getActivityFacilities().addActivityFacility(xfacility);
		}
		Id<Link> hlink = NetworkUtils.getNearestLink(carNetwork, coord).getId();
		Activity home1 = PopulationUtils.createAndAddActivityFromCoord(plan, type, coord);
		home1.setLinkId(hlink);
		home1.setFacilityId(xfacility.getId());
		return home1;
	}
	
}
