package org.matsim.haitam.api.qsim;

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
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.core.CarsharingBookingRecord;
import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.carsharing.core.CarsharingStationMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingVehicleTrip;
import org.matsim.haitam.api.events.CarsharingBookingEvent;
import org.matsim.haitam.api.events.CarsharingChargingEndEvent;
import org.matsim.haitam.api.events.CarsharingDropoffVehicleEvent;
import org.matsim.haitam.api.events.CarsharingPickupVehicleEvent;
import org.matsim.haitam.api.router.CarsharingNearestStationRouterModule;
import org.matsim.haitam.api.router.CarsharingNearestStationRouterModule.CarsharingLocationInfo;
import org.matsim.haitam.api.router.CarsharingRouterModeCst;
import org.matsim.haitam.api.utils.CarsharingUtils;

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
		ArrayList<CarsharingOffer> offers = this.carsharingSystemDelegate.booking().offer().computeRentalOffers(now, demand);
		
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
		List<? extends PlanElement> elements = 
				this.tripRouter.calcRoute(TransportMode.car, start, end, deptime, this.customerAgentMemory.getPerson());
		double tt = CarsharingUtils.calcDuration(elements);
		Boolean novehiclefound = true;
		Boolean noparkingfound = true;
		for(CarsharingOffer o : offers) {
			if(o.hasValidAccess()) { novehiclefound = false; break;	}
		}
		CarsharingBookingRecord booking = CarsharingBookingRecord.constructAndGetFailedBookingRec(
				now, demand, 
				novehiclefound,	departure.station, now,
				noparkingfound,	arrival.station, deptime + tt);
		return booking;
	}
	

	@Override
	public Leg prepareAccessWalk(double now) {
		return CarsharingUtils.createWalkLeg(
				CarsharingRouterModeCst.cs_access_walk, 
				CarsharingUtils.getDummyFacility(this.currBookingRecord.getRelatedOffer().getDemand().getOrigin()), 
				this.currBookingRecord.getRelatedOffer().getAccess().getStation().facility(), 
				this.currBookingRecord.getRelatedOffer().getAccess().getTravelTime(), 
				this.currBookingRecord.getRelatedOffer().getAccess().getDistance());
	}

	@Override
	public Leg prepareEgressWalk(double now) {
		return CarsharingUtils.createWalkLeg(
				CarsharingRouterModeCst.cs_egress_walk, 
				this.currBookingRecord.getRelatedOffer().getEgress().getStation().facility(), 
				CarsharingUtils.getDummyFacility(this.currBookingRecord.getRelatedOffer().getDemand().getDestination()), 
				this.currBookingRecord.getRelatedOffer().getEgress().getTravelTime(), 
				this.currBookingRecord.getRelatedOffer().getEgress().getDistance());
	}

	
	@Override
	public Activity prepareAccessStation(double now) {
		return CarsharingUtils.createAccessStationActivity(
				this.currBookingRecord.getOriginStation().facility(), now, this.currBookingRecord.getRelatedOffer().getAccess().getOffsetDur());
	}

	@Override
	public Activity prepareEgressStation(double now) {
		return CarsharingUtils.createEgressStationActivity(
				this.currBookingRecord.getDestinationStation().facility(), now, this.currBookingRecord.getRelatedOffer().getEgress().getOffsetDur());
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
