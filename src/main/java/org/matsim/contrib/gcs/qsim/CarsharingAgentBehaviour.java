package org.matsim.contrib.gcs.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleTrip;
import org.matsim.contrib.gcs.events.CarsharingBookingEvent;
import org.matsim.contrib.gcs.events.CarsharingChargingEndEvent;
import org.matsim.contrib.gcs.events.CarsharingDropoffVehicleEvent;
import org.matsim.contrib.gcs.events.CarsharingPickupVehicleEvent;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;
import org.matsim.contrib.gcs.router.CarsharingNearestStationRouterModule.CarsharingLocationInfo;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;

public class CarsharingAgentBehaviour extends AbstractCarsharingAgentBehaviour {

	private static Logger logger = Logger.getLogger(CarsharingAgentBehaviour.class);
	CarsharingNearestStationRouterModule nearStationRouter;
	public CarsharingAgentBehaviour(Plan plan, Netsim simulation, TripRouter tripRouter, CarsharingManager manager) {
		super(plan, simulation, tripRouter, manager);
		this.nearStationRouter = new CarsharingNearestStationRouterModule(this.basicAgentDelegate.getScenario(), this.carsharingSystemDelegate, null);
	}
	
	@Override
	public void pickup(double now) {
		
		CarsharingStationMobsim  accessStation = this.currBookingRecord.getRelatedOffer().getAccess().getStation();
		CarsharingVehicleMobsim vehicle = accessStation.pickup(this.customerAgentMemory, this.currBookingRecord.getNbrOfVeh(), now);
		Queue<CarsharingVehicleMobsim> roadTrain = null;
		if(vehicle != null) {
			roadTrain = vehicle.roadTrain();
			if(!vehicle.battery().isFullyCharged()) {
				this.basicAgentDelegate.getEvents().processEvent(
						new CarsharingChargingEndEvent(
								now,	
								this.basicAgentDelegate.getScenario(), 
								this.carsharingSystemDelegate, 
								accessStation,
								roadTrain, 
								now));	
			}
			
			this.carsharingSystemDelegate.
				booking().
				track(accessStation).
				confirm(this.currBookingRecord, vehicle);

			/*for(CarsharingVehicleMobsim v : roadTrain) {
				this.customerAgentMemory.status().getOngoingRental().setTrip(v.status().getTrip().getId());
			}*/
			
		} else {
			logger.warn("FAILURE TO PICKUP ... ABORTING...");
			this.setStateToAbort(now);
		}
		
		this.basicAgentDelegate.getEvents().processEvent(
				new CarsharingPickupVehicleEvent(
						now, 
						this.basicAgentDelegate.getScenario(), 
						this.carsharingSystemDelegate, 
						this.customerAgentMemory, 
						accessStation, 
						roadTrain,
						this.currBookingRecord));

	}

	@Override
	public void dropoff(double now) {
		CarsharingStationMobsim  egressStation = this.currBookingRecord.getRelatedOffer().getEgress().getStation();
		
		if(!this.carsharingSystemDelegate.getStations().map().containsKey(egressStation.facility().getId())) {
			// in case of newly created freefloating station
			this.carsharingSystemDelegate.getStations().add(egressStation);
		}
		
		CarsharingVehicleMobsim vehicle = this.currBookingRecord.getVehicle();
		CarsharingVehicleTrip ongoingTrip = vehicle.status().getTrip(); 
		Queue<CarsharingVehicleMobsim> roadTrain = vehicle.roadTrain();
		
		if(egressStation.dropoff(this.customerAgentMemory, vehicle, now)) {
			this.carsharingSystemDelegate.
			booking().
			track(egressStation).
			confirm(this.currBookingRecord, vehicle);
			/*for(CarsharingVehicleMobsim v : roadTrain) {
				this.customerAgentMemory.status().getOngoingRental().setPark(v.status().getPark().getId());
			}*/
		} else {
			logger.warn("FAILURE TO DROPOFF ... ABORTING...");
			this.setStateToAbort(now);
		}
		
		ongoingTrip.setRentalCost(this.carsharingSystemDelegate.booking().
				offer().computeRentalCost(this.currBookingRecord.getRelatedOffer(), ongoingTrip.getRentalDuration()));
		
		
		this.basicAgentDelegate.getEvents().processEvent(
				new PersonMoneyEvent(
						now,
						this.customerAgentMemory.getPerson().getId(),
						ongoingTrip.getRentalCost()));
		
		this.basicAgentDelegate.getEvents().processEvent(
				new CarsharingDropoffVehicleEvent(
						now, 
						this.basicAgentDelegate.getScenario(), 
						this.carsharingSystemDelegate, 
						this.customerAgentMemory,
						egressStation, 
						roadTrain,
						this.currBookingRecord));
	}

	@Override
	public CarsharingBookingRecord book(double now, Leg accessWalkLeg) {
		
		// EARLY BOOKING
		CarsharingBookingRecord booking = this.carsharingSystemDelegate.booking().getRecord(accessWalkLeg);
		if(booking != null)	return booking;
		
		// IMMEDIATE BOOKING
		CarsharingDemand demand = null;
		
		// [CUSTOMER] Get Demand from Person
		demand = this.customerAgentMemory.decision().getOrConstructDemand(accessWalkLeg, this.basicAgentDelegate.getCurrentPlan());
		
		// [SYSTEM] Get Full Trips Offers
		ArrayList<CarsharingOffer> offers = this.carsharingSystemDelegate.booking().offer().computeRentalOffers((int)now, demand);
		
		// [CUSTOMER] Choose an Offer
		CarsharingOffer selectedOffer = this.customerAgentMemory.decision().selectOffer(offers);
		
		if(selectedOffer == null) { // IN CASE OF NO SELECTED OFFER
			booking = constructFailedRecord(now, offers, demand);
		} else { // IN CASE OF SELECTED OFFER
			booking = CarsharingBookingRecord.constructAndGetBookingRec(now, selectedOffer);
		}
		
		// [SYSTEM] Confirm Booking
		this.carsharingSystemDelegate.booking().process(booking);
		
		// [CUSTOMER] Booking Confirmation Feedback
		this.customerAgentMemory.decision().acceptBooking(booking, now);
		
		// [SYSTEM]
		this.basicAgentDelegate.getEvents().processEvent(
				new CarsharingBookingEvent(
						now, 
						this.basicAgentDelegate.getScenario(), 
						this.carsharingSystemDelegate, 
						demand,
						booking));
		 
		return booking;
	}
	
	private CarsharingBookingRecord constructFailedRecord(double now, ArrayList<CarsharingOffer> offers, CarsharingDemand demand) {
		CarsharingLocationInfo departure = this.nearStationRouter.getNearestStationToDeparture(CarsharingUtils.getDummyFacility(demand.getOrigin()), true);
		CarsharingLocationInfo arrival = this.nearStationRouter.getNearestStationToArrival(CarsharingUtils.getDummyFacility(demand.getDestination()), true, departure);
		Facility start = departure.facility;
		Facility end = arrival.facility;
		double deptime = now;
		if(departure.station != null) {
			start = departure.station.facility();
			deptime += departure.traveltime + this.carsharingSystemDelegate.getConfig().getInteractionOffset();
		}
		if(arrival.station != null){
			end = arrival.station.facility();
		}
		
		/*CarsharingNearestStationRouterModule csrouter = 
				new CarsharingNearestStationRouterModule(
						this.basicAgentDelegate.getScenario(), 
						this.carsharingSystemDelegate, 
						CarsharingPlanModeCst.directTrip);
		List<? extends PlanElement> default_route = csrouter.calcRoute(start, end, deptime, this.customerAgentMemory.getPerson());*/
		//RouteData rd = CarsharingRouterUtils.calcTCC(this.carsharingSystemDelegate, start, end, deptime, this.customerAgentMemory.getPerson());
		Boolean novehiclefound = true;
		Boolean noparkingfound = true;
		boolean betterWalk = false;
		for(CarsharingOffer o : offers) {
			if(!o.hasValidAccess() && o.getAccess().getStatus().equals(CarsharingOffer.FAILURE_WALK_OFFER)) {
				betterWalk = true;
				break;
			} else if(o.hasValidAccess()) { 
				novehiclefound = false; 
				break;	
			} 
		}
		CarsharingBookingRecord booking = CarsharingBookingRecord.constructAndGetFailedBookingRec(
				now, demand, betterWalk,
				novehiclefound,	departure.station, deptime,
				noparkingfound,	arrival.station, Double.NaN);
		return booking;
	}
	

	@Override
	public Leg prepareAccessWalk(double now) {
		return CarsharingUtils.createWalkLeg(
				CarsharingRouterUtils.cs_access_walk, 
				CarsharingUtils.getDummyFacility(this.currBookingRecord.getRelatedOffer().getDemand().getOrigin()), 
				this.currBookingRecord.getRelatedOffer().getAccess().getStation().facility(), 
				this.currBookingRecord.getRelatedOffer().getAccess().getTravelTime(), 
				this.currBookingRecord.getRelatedOffer().getAccess().getDistance());
	}

	@Override
	public Leg prepareEgressWalk(double now) {
		return CarsharingUtils.createWalkLeg(
				CarsharingRouterUtils.cs_egress_walk, 
				this.currBookingRecord.getRelatedOffer().getEgress().getStation().facility(), 
				CarsharingUtils.getDummyFacility(this.currBookingRecord.getRelatedOffer().getDemand().getDestination()), 
				this.currBookingRecord.getRelatedOffer().getEgress().getTravelTime(), 
				this.currBookingRecord.getRelatedOffer().getEgress().getDistance());
	}

	
	@Override
	public Activity prepareAccessStation(double now) {
		return CarsharingUtils.createAccessStationActivity(
				this.currBookingRecord.getOriginStation().facility(), now, this.carsharingSystemDelegate.getConfig().getInteractionOffset());
	}

	@Override
	public Activity prepareEgressStation(double now) {
		return CarsharingUtils.createEgressStationActivity(
				this.currBookingRecord.getDestinationStation().facility(), now, this.carsharingSystemDelegate.getConfig().getInteractionOffset());
	}
	
	@Override
	public Leg prepareDrive(double now, Facility Odrive, Facility Ddrive) {
		return CarsharingUtils.createDriveLeg(Odrive, Ddrive, 
				this.currBookingRecord.getRelatedOffer().getDrive().getRoute(), null);
	}
	
	
	@Override
	public void default_endActivityAndComputeNextState(double now) {

	}
	
	@Override
	public void default_endLegAndComputeNextState(double now) {
		
	}


		
}
