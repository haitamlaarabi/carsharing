package org.matsim.contrib.gcs.examples;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.carsharing.CarsharingScenario;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;

public class CarsharingDemandGenerator {
	
	// Default behavior, customers will be living and working within a radius of 500m
	public static void dummy(Scenario scenario, CarsharingScenario carsharing, int demandSize) {
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
			work.setStartTime(8 * 3600 + 30 * 60);
			work.setEndTime(17 * 3600);
			ActivityFacility wfacility = factory.createActivityFacility(Id.create((facility_counter++)+"w", ActivityFacility.class), w);
			work.setFacilityId(wfacility.getId());
			
			// leg
			Leg leg2 = PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home
			Activity home2 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", h);
			home2.setStartTime(17 * 3600 + 30 * 60);
			home2.setLinkId(hlink);
			home2.setFacilityId(hfacility.getId());
			

			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			scenario.getActivityFacilities().addActivityFacility(hfacility);
			scenario.getActivityFacilities().addActivityFacility(wfacility);
		}
	}
	
	static void scenario_user_relocation(Scenario scenario, CarsharingScenario carsharing) {
		
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
	
	protected static Activity createAndGetActivity(Scenario scenario, Network carNetwork, Plan plan, String type, Coord coord, String id) {
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
