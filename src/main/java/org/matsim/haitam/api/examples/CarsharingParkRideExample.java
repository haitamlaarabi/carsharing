package org.matsim.haitam.api.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.haitam.api.carsharing.CarsharingScenario;
import org.matsim.haitam.api.carsharing.core.CarsharingStation;
import org.matsim.haitam.api.config.CarsharingInstaller;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;
import org.matsim.haitam.api.utils.CarsharingUtils;

public class CarsharingParkRideExample extends  CarsharingExample {

	public static void main(String[] args) {	
		new CarsharingParkRideExample().run("CarsharingParkRideExample", "carsharing_scenario1.xml");
	}
	
	@Override
	protected void modulesToInstance(CarsharingInstaller installer) {
		installer.carsharing.getConfig().setCarsharingScenarioInputFile(rootDir + "/input/carsharing.xml");
		installer.carsharing.getConfig().setActivateModule(true);
		installer.carsharing.getConfig().setRentalRatePerMin(-0.0);
		installer.carsharing.getConfig().setSearchDistance(500.0);
	}
	
	@Override
	protected void generateCarsharingDemand(Scenario scenario, CarsharingScenario carsharing, int demandSize) {
		
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		for(int i = 0 ; i < 1; i++) {
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("c"+i));
			Plan plan = PopulationUtils.createPlan(p);
	
			// home1
			CarsharingStation start_station = carsharing.getStations().get("stat.id.1");
			CarsharingStation dest_station = carsharing.getStations().get("stat.id.2");
	
			Coord h = getRandomCoordInDisk(start_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Id<Link> hlink = NetworkUtils.getNearestLink(scenario.getNetwork(), h).getId();
			Activity home1 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", h);
			home1.setEndTime(8 * 3600);
			home1.setLinkId(hlink);
			ActivityFacility hfacility = factory.createActivityFacility(Id.create("h"+i, ActivityFacility.class), h);
			home1.setFacilityId(hfacility.getId());
			
			// leg cs 1
			Leg legCS1 = PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// park & ride 1
			Coord pr = getRandomCoordInDisk(dest_station.facility().getCoord(), carsharing.getConfig().getSearchDistance());
			Activity parkride1 = PopulationUtils.createAndAddActivityFromCoord(plan, "parkride", pr);
			Id<Link> prlink = NetworkUtils.getNearestLink(scenario.getNetwork(), pr).getId();
			parkride1.setLinkId(prlink);
			parkride1.setMaximumDuration(1.0);
			ActivityFacility prfacility = factory.createActivityFacility(Id.create("pr"+i, ActivityFacility.class), pr);
			parkride1.setFacilityId(prfacility.getId());
			
			// leg pt 1
			Leg legPT1 = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
			
			// work
			Id<Node> idnode3 = Id.createNodeId(3);
			Coord w = getRandomCoordInDisk(scenario.getNetwork().getNodes().get(idnode3).getCoord(), carsharing.getConfig().getSearchDistance());
			Activity work = PopulationUtils.createAndAddActivityFromCoord(plan, "w", w);
			Id<Link> wlink = NetworkUtils.getNearestLink(scenario.getNetwork(), w).getId();
			work.setLinkId(wlink);
			work.setEndTime(17 * 3600);
			ActivityFacility wfacility = factory.createActivityFacility(Id.create("w"+i, ActivityFacility.class), w);
			work.setFacilityId(wfacility.getId());
			
			// leg pt 2
			Leg legPT2 = PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
			
			
			// park & ride 2
			Activity parkride2 = PopulationUtils.createAndAddActivityFromCoord(plan, "parkride", pr);
			parkride2.setLinkId(prlink);
			parkride2.setStartTime(17 * 3600.0 + 15 * 60.0);
			parkride2.setMaximumDuration(1.0);
			parkride2.setFacilityId(prfacility.getId());
			
			// leg cs 2
			Leg legCS2 = PopulationUtils.createAndAddLeg(plan, CarsharingPlanModeCst.directTrip);
			
			// home2
			Activity home2 = PopulationUtils.createAndAddActivityFromCoord(plan, "h", h);
			home2.setLinkId(hlink);
			home2.setFacilityId(hfacility.getId());
			home2.setStartTime(18 * 3600);
	
	
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
			scenario.getActivityFacilities().addActivityFacility(hfacility);
			scenario.getActivityFacilities().addActivityFacility(wfacility);
			scenario.getActivityFacilities().addActivityFacility(prfacility);
		}
		
		{
			ActivityParams station = new ActivityParams("parkride");
			station.setClosingTime(CarsharingUtils.toSecond(23,59,59));
			station.setOpeningTime(CarsharingUtils.toSecond(0,0,0));
			station.setTypicalDuration(CarsharingUtils.toSecond(0,1,0));
			scenario.getConfig().planCalcScore().addActivityParams(station);
		}
	}
}
