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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.facilities.ActivityFacility;
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
		this.mng = manager;
		this.sc = sc;
		this.net = this.sc.getNetwork();
		this.rentals = new HashMap<Id<Person>, RentalTracker>();
		this.trips = new HashMap<Id<Vehicle>, LinkTracker>(); 
	}
	
	@Override
	public void reset(int iteration) {
		this.rentals.clear();
		this.trips.clear();
	}

	/* ****** LINK EVENTS ****** */

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.trips.containsKey(event.getVehicleId())) {
			LinkTracker lt = this.trips.get(event.getVehicleId());
			lt.time = event.getTime();
			lt.link = this.net.getLinks().get(event.getLinkId());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkTracker lt = this.trips.get(event.getVehicleId());
		if(lt != null && lt.link != null) {
			double traveltime = event.getTime() - lt.time;
			double traveldist = lt.link.getLength();
			this.mng.vehicles().map().get(event.getVehicleId()).drive(traveltime, traveldist);
			lt.rt.trip_dist += traveldist;
		}
	}


	/* ****** VEHICLE EVENTS ****** */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.rentals.containsKey(event.getPersonId())) {
			RentalTracker rt = this.rentals.get(event.getPersonId());
			rt.idv = event.getVehicleId();
			LinkTracker lt = new LinkTracker(event.getTime(), rt);
			this.trips.put(lt.rt.idv, lt);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		LinkTracker lt = this.trips.get(event.getVehicleId());
		if(lt != null && lt.link != null) {
			double traveltime = event.getTime() - lt.time;
			double traveldist = lt.link.getLength(); // half distance, arrived
			this.mng.vehicles().map().get(event.getVehicleId()).drive(traveltime, traveldist);
			lt.rt.trip_dist += traveldist;
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		ActivityFacility f = this.sc.getActivityFacilities().getFacilities().get(event.getFacilityId());
		if(f != null && this.mng.getStations().map().containsKey(f.getId())) { 
			// car sharing station
			if(!this.rentals.containsKey(event.getPersonId())) {
				// access station
				RentalTracker rt = new RentalTracker();
				rt.idp = event.getPersonId();
				rt.start_link = this.net.getLinks().get(event.getLinkId());
				rt.start_time = event.getTime();
				this.rentals.put(event.getPersonId(), rt);
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		ActivityFacility f = this.sc.getActivityFacilities().getFacilities().get(event.getFacilityId());
		if(f != null && this.mng.getStations().map().containsKey(f.getId())) { 
			RentalTracker rt = this.rentals.get(event.getPersonId());
			if(rt != null && rt.idv != null) {
				// egress station			
				rt.end_time = event.getTime();
				rt.end_link = this.net.getLinks().get(event.getLinkId());
				// CONCLUDING RENTAL
				
				this.trips.remove(rt.idv);
				this.rentals.remove(rt.idp);
				
			}
		}
	}
	

	/* ****** PERSON EVENTS ****** */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
	}
	
	@Override
	public void handleEvent(VehicleAbortsEvent event) {
	}

	private final Network net;
	private final Scenario sc;
	private final CarsharingManager mng;
	private final HashMap<Id<Person>, RentalTracker> rentals;
	private final HashMap<Id<Vehicle>, LinkTracker> trips;
	
	class RentalTracker {
		public double trip_dist = 0;
		public double start_time;
		public double end_time;
		public Link start_link;
		public Link end_link;
		public Id<Person> idp;
		public Id<Vehicle> idv = null;
	}
	
	class LinkTracker {
		RentalTracker rt;
		public Link link = null;
		public double time;
		public LinkTracker(double time, RentalTracker rt) {
			this.time = time;
			this.rt = rt;
		}
	}
	
}
