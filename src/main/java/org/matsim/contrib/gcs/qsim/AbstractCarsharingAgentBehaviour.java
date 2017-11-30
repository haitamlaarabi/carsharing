package org.matsim.contrib.gcs.qsim;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.contrib.gcs.router.CarsharingRouterModeCst;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.VehicleUsingAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public abstract class AbstractCarsharingAgentBehaviour implements 	VehicleUsingAgent, 
															MobsimDriverAgent, 
															MobsimPassengerAgent, 
															HasPerson, 
															PlanAgent, 
															PTPassengerAgent{

	
	protected BasicPlanAgentImpl basicAgentDelegate;
	protected TransitAgentImpl transitAgentDelegate;
	protected PlanBasedDriverAgentImpl driverAgentDelegate;
	protected QSim qSim;
	protected TripRouter tripRouter;
	
	protected CarsharingManager carsharingSystemDelegate; // System
	protected CarsharingCustomerMobsim customerAgentMemory; // Agent
	protected CarsharingBookingRecord currBookingRecord; // booking
	
	public AbstractCarsharingAgentBehaviour(final Plan plan, final Netsim simulation, TripRouter tripRouter, CarsharingManager manager) {
		this.basicAgentDelegate = new BasicPlanAgentImpl( plan, simulation.getScenario(), simulation.getEventsManager(), simulation.getSimTimer() ) ;
		this.transitAgentDelegate = new TransitAgentImpl( this.basicAgentDelegate ) ;
		this.driverAgentDelegate = new PlanBasedDriverAgentImpl( this.basicAgentDelegate ) ;
		this.basicAgentDelegate.getModifiablePlan() ; // this lets the agent make a full copy of the plan, which can then be modified
		this.qSim = (QSim) simulation;
		this.tripRouter = tripRouter;
		this.carsharingSystemDelegate = manager;
		this.customerAgentMemory = this.carsharingSystemDelegate.customers().map().get(this.getId());
	}
	
	public abstract void pickup(double now);
	public abstract void dropoff(double now);
	public abstract CarsharingBookingRecord book(double now, Leg accessWalkLeg);
	
	public abstract Leg prepareDrive(double now, Facility o, Facility d);
	public abstract Leg prepareAccessWalk(double now);
	public abstract Leg prepareEgressWalk(double now);
	public abstract Activity prepareAccessStation(double now);
	public abstract Activity prepareEgressStation(double now);
	
	public abstract void default_endActivityAndComputeNextState(double now);
	public abstract void default_endLegAndComputeNextState(double now);
		
	
	// ####################################################################
	// WORKFLOW
	
	@Override
	public void endActivityAndComputeNextState(double now) {
		default_endActivityAndComputeNextState(now);
		
		if(this.customerAgentMemory != null) {
			PlanElement currElem = this.basicAgentDelegate.getCurrentPlanElement();
			PlanElement nextElem = this.basicAgentDelegate.getNextPlanElement();
			
			if(CarsharingUtils.isUnRoutedCarsharingLeg(nextElem)) {
				this.currBookingRecord = book(now, (Leg)nextElem);
				if(this.currBookingRecord.bookingFailed()) { 
					switchModeAfterBookingFailed(now, (Leg)nextElem);
				} else {
					constructCarsharingTripAfterBookingSucceeded(now, (Leg)nextElem);
				}
			}
			
			if(CarsharingUtils.isEgressStation(currElem)) {
				teleportTrailerBeforeDropoff(((Activity)currElem).getLinkId());
				dropoff(now);
			}
			
			if(CarsharingUtils.isAccessWalk(nextElem)) {
			} else if (CarsharingUtils.isDrive(nextElem)) {
				NetworkRoute route = (NetworkRoute) ((Leg)nextElem).getRoute();
				route.setVehicleId(getCSVehicleId());
			} else if (CarsharingUtils.isEgressWalk(nextElem)) {
			} 
		}
		
		if (!this.getState().equals(State.ABORT)) {
			this.basicAgentDelegate.endActivityAndComputeNextState(now);
		} 
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		default_endLegAndComputeNextState(now);
		
		if(this.customerAgentMemory != null) {
			PlanElement nextElem = this.basicAgentDelegate.getNextPlanElement();
			
			if(CarsharingUtils.isUnRoutedCarsharingLeg(nextElem)) {
				this.currBookingRecord = book(now, (Leg)nextElem);
				if(this.currBookingRecord.bookingFailed()) { 
					switchModeAfterBookingFailed(now, (Leg)nextElem);
				} else {
					constructCarsharingTripAfterBookingSucceeded(now, (Leg)nextElem);
				}
			}
			
			if(CarsharingUtils.isAccessWalk(nextElem)) {
			} else if (CarsharingUtils.isDrive(nextElem)) {
				NetworkRoute route = (NetworkRoute) ((Leg)nextElem).getRoute();
				route.setVehicleId(getCSVehicleId());
			} else if (CarsharingUtils.isEgressWalk(nextElem)) {
			} else if(CarsharingUtils.isAccessStation(nextElem)) {
				pickup(now);
			} 
		}
		
		if (!this.getState().equals(State.ABORT)) {
			this.basicAgentDelegate.endLegAndComputeNextState(now);
		}
	}
		
	private void teleportTrailerBeforeDropoff(Id<Link> idlink) {
		for(CarsharingVehicleMobsim v : getCSVehicle().roadTrain()) {
			if(v != getCSVehicle()) {
				qSim.createAndParkVehicleOnLink(v.vehicle(), idlink);
			}
		}
	}
	
	private void constructCarsharingTripAfterBookingSucceeded(double now, Leg unroutedPlanElement) {
		
		List<PlanElement> planelements = this.getCurrentPlan().getPlanElements();
		int unroutedIndex = planelements.indexOf(unroutedPlanElement);
		Activity O = (Activity) planelements.get(unroutedIndex - 1);
		Activity D = (Activity) planelements.get(unroutedIndex + 1);
		Facility Odrive = CarsharingUtils.getDummyFacility(O);
		Facility Ddrive = CarsharingUtils.getDummyFacility(D);
		
		String mode = unroutedPlanElement.getMode();
		
		Activity accessStation = null, egressStation = null;
		if(mode.equals(CarsharingPlanModeCst.directTrip) || mode.equals(CarsharingPlanModeCst.startTrip)) {
			accessStation = prepareAccessStation(now);
			Odrive = CarsharingUtils.getDummyFacility(accessStation);
		}
		if(mode.equals(CarsharingPlanModeCst.directTrip) || mode.equals(CarsharingPlanModeCst.endTrip)) {
			egressStation = prepareEgressStation(now);
			Ddrive = CarsharingUtils.getDummyFacility(egressStation);
		}
		
		List<PlanElement> cstrip = new ArrayList<PlanElement>();
		double tt = now;

		if(accessStation != null) {	
			Leg accesswalk = prepareAccessWalk(now);
			tt += accesswalk.getTravelTime() + accessStation.getMaximumDuration();
			cstrip.add(accesswalk);
			cstrip.add(accessStation);
		}
				
		Leg drive = prepareDrive(tt, Odrive, Ddrive); 
		tt += drive.getTravelTime();
		cstrip.add(drive);
		
		if(egressStation != null) { 
			tt += egressStation.getMaximumDuration();
			cstrip.add(egressStation);
			cstrip.add(prepareEgressWalk(tt));
		}
		
		TripRouter.insertTrip(this.getCurrentPlan(), O,	cstrip, D);
	}
	
	private void switchModeAfterBookingFailed(double now, Leg unroutedPlanElement) {
		List<PlanElement> planelements = this.getCurrentPlan().getPlanElements();
		int unroutedIndex = planelements.indexOf(unroutedPlanElement);
		Activity O = (Activity) planelements.get(unroutedIndex - 1);
		Activity D = (Activity) planelements.get(unroutedIndex + 1);
		List<PlanElement> new_trip = new ArrayList<PlanElement>();
		//Leg new_leg = null;
		if(this.currBookingRecord.betterWalk()) {
			//new_leg = PopulationUtils.createLeg(CarsharingRouterModeCst.cs_walk);
			new_trip.addAll(this.tripRouter.calcRoute(
					CarsharingRouterModeCst.cs_walk, 
					CarsharingUtils.getDummyFacility(O), CarsharingUtils.getDummyFacility(D), 
					now, this.basicAgentDelegate.getPerson()));
		} else {
			//new_leg = PopulationUtils.createLeg(CarsharingRouterModeCst.cs_pt);
			new_trip.addAll(this.tripRouter.calcRoute(
					CarsharingRouterModeCst.cs_pt, 
					CarsharingUtils.getDummyFacility(O), CarsharingUtils.getDummyFacility(D), 
					now, this.basicAgentDelegate.getPerson()));
		}
		//new_leg.setRoute(new LinkNetworkRouteImpl(O.getLinkId(), D.getLinkId()));
		//new_trip.add(new_leg);
		TripRouter.insertTrip(this.getCurrentPlan(), O, new_trip, D);
	}

	
	public Id<Vehicle> getCSVehicleId() {
		CarsharingVehicleMobsim v = getCSVehicle();
		if(v != null) {
			return v.getId();
		}
		return null;
	}
	
	public CarsharingVehicleMobsim getCSVehicle() {
		if(this.currBookingRecord != null && this.currBookingRecord.getVehicle() != null) {
			return this.currBookingRecord.getVehicle();
		}
		return null;
	}
	
	// ####################################################################
	// every agent use a vehicle with same ID as agent's
	
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) 
			return route.getVehicleId();
		else
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
	}
	
	
	// ####################################################################
	// only pure delegate methods below this line

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.basicAgentDelegate.getCurrentPlanElement() ;
	}

	@Override
	public final PlanElement getNextPlanElement() {
		return this.basicAgentDelegate.getNextPlanElement() ;
	}

	@Override
	public final void setVehicle(final MobsimVehicle veh) {
		this.basicAgentDelegate.setVehicle(veh) ;
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return this.basicAgentDelegate.getVehicle() ;
	}

	@Override
	public final double getActivityEndTime() {
		return this.basicAgentDelegate.getActivityEndTime() ;
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return this.driverAgentDelegate.getCurrentLinkId() ;
	}

	@Override
	public final Double getExpectedTravelTime() {
		return this.basicAgentDelegate.getExpectedTravelTime() ;

	}

	@Override
	public Double getExpectedTravelDistance() {
		return this.basicAgentDelegate.getExpectedTravelDistance() ;
	}

	@Override
	public final String getMode() {
		return this.basicAgentDelegate.getMode() ;
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.basicAgentDelegate.getDestinationLinkId() ;
	}

	@Override
	public final Person getPerson() {
		return this.basicAgentDelegate.getPerson() ;
	}

	@Override
	public final Id<Person> getId() {
		return this.basicAgentDelegate.getId() ;
	}

	@Override
	public final Plan getCurrentPlan() {
		return this.basicAgentDelegate.getCurrentPlan() ;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return this.transitAgentDelegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle) ;
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return this.transitAgentDelegate.getExitAtStop(stop) ;
	}

	@Override
	public double getWeight() {
		return this.transitAgentDelegate.getWeight() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return this.driverAgentDelegate.isWantingToArriveOnCurrentLink() ;
	}

	@Override
	public MobsimAgent.State getState() {
		return this.basicAgentDelegate.getState() ;
	}
	@Override
	public final void setStateToAbort(final double now) {
		this.basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		this.driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.driverAgentDelegate.chooseNextLinkId() ;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	@Override
	public PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

}
