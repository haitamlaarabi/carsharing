package org.matsim.contrib.gcs.control;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.vehicles.Vehicle;


public class AgentEventsListener implements PersonLeavesVehicleEventHandler, 
											PersonEntersVehicleEventHandler, 
											PersonArrivalEventHandler, 
											PersonDepartureEventHandler,
											LinkEnterEventHandler,
											LinkLeaveEventHandler,
											ActivityStartEventHandler,
											ActivityEndEventHandler,
											VehicleAbortsEventHandler
{

	public AgentEventsListener(Scenario sc, CarsharingManager manager) {
		this.manager = manager;
		this.scenario = sc;
		this.network = this.scenario.getNetwork();
		this.linktimeMap = new HashMap<Id<Vehicle>, Double>();
	}
	
	@Override
	public void reset(int iteration) {
		this.linktimeMap.clear();
	}

	/* ****** LINK EVENTS ****** */

	@Override
	public void handleEvent(LinkEnterEvent event) {
		CarsharingVehicleMobsim carsharingVeh = manager.vehicles().map().get(event.getVehicleId());
		if (carsharingVeh != null) {
			linktimeMap.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		CarsharingVehicleMobsim carsharingVeh = manager.vehicles().map().get(event.getVehicleId());
		if (carsharingVeh != null) {
			double realspeed = 0;
			double realdistance = 0;
			Link link = this.network.getLinks().get(event.getLinkId());
			
			double mobsimTravelTime = event.getTime() - linktimeMap.get(event.getVehicleId());
			//double vehMaxSpeed = link.getFreespeed();
			double vehMaxSpeed = carsharingVeh.vehicle().getType().getMaximumVelocity();
			double minTravelTime = link.getLength()/vehMaxSpeed;
			
			if(mobsimTravelTime < minTravelTime) {
				realspeed = vehMaxSpeed;
				realdistance = realspeed * mobsimTravelTime;
			} else {
				realdistance = link.getLength();
				realspeed = realdistance/mobsimTravelTime;
			}
			
			carsharingVeh.drive(realdistance, realspeed, vehMaxSpeed);
		}
	}

	/* ****** PERSON EVENTS ****** */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
	}

	/* ****** VEHICLE EVENTS ****** */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		CarsharingCustomerMobsim carsharingCustomer = manager.customers().map().get(event.getPersonId());
		if(carsharingCustomer != null) {
			CarsharingBookingRecord bookRec = carsharingCustomer.status().getOngoingRental();
			if(bookRec != null && bookRec.getVehicle() != null) {
				linktimeMap.put(bookRec.getVehicle().vehicle().getId(), event.getTime());
			}
		}
	}
		
	private final Network network;
	private final Scenario scenario;
	private final CarsharingManager manager;
	private final HashMap<Id<Vehicle>, Double> linktimeMap;
	
}
