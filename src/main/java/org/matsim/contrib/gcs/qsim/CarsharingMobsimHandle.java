package org.matsim.contrib.gcs.qsim;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.contrib.gcs.events.AbstractCarsharingEvent;
import org.matsim.contrib.gcs.events.CarsharingDropoffVehicleEvent;
import org.matsim.contrib.gcs.events.CarsharingPickupVehicleEvent;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

public abstract class CarsharingMobsimHandle implements MobsimEngine, DepartureHandler, ActivityHandler, MatsimExtensionPoint {
	
	private static Logger logger = Logger.getLogger(CarsharingMobsimHandle.class);
	@Inject protected CarsharingManager m;
	@Inject protected TripRouter tripRouter;
	@Inject protected ScoringFunctionFactory scoringFunctionFactory;
	@Inject protected Scenario sc;
	protected double iteration;
	protected QSim qSim;
	protected CarsharingRelocationModel relocation;
	protected CarsharingDataCollector collector;
	private PriorityQueue<CarsharingRelocationTask> relocationEventsQueue = null;
	
	protected abstract void execute(double time);

	
	@Override
	public void doSimStep(double time) {
		this.relocation.updateRelocationList(time);
		this.relocationEventsQueue.addAll(this.relocation.relocationList(time));
		if ((this.relocationEventsQueue != null) && (this.relocationEventsQueue.size() > 0)) {
			handleRelocationEvents(time);
		}
		for(CarsharingDataProvider d : this.collector.getAllModules()) {
			this.collector.addLog(d, time);
		}
		this.execute(time);
	}	

	@Override
	public void onPrepareSim() {
		this.relocationEventsQueue = new PriorityQueue<CarsharingRelocationTask>();
		this.relocation = this.m.relocation();
		this.collector = this.m.dataCollector();
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.qSim = (QSim) internalInterface.getMobsim();
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		return false;
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		return false;
	}
	
	private void handleRelocationEvents(final double time) {
		while ((this.relocationEventsQueue.size() > 0) && (this.relocationEventsQueue.peek().getTime() <= time)) {
			CarsharingRelocationTask task = this.relocationEventsQueue.poll();
			CarsharingOperatorMobsim op = (CarsharingOperatorMobsim) task.getAgent();
			Queue<CarsharingVehicleMobsim> train = null;
			AbstractCarsharingEvent event = null;
			boolean book = false;
			if(task.getType().equals("START")) { 
				book = op.decision().processPickup(time, task);
				if(op.getVehicle() != null) {  // vehicles picked up
					train = op.getVehicle().roadTrain(); 
				} 				
				event = new CarsharingPickupVehicleEvent(
								task.getTime(), qSim.getScenario(), 
								m, task.getAgent(), task.getStation(), 
								train, task.getBooking());
				
			}  else {
				if(op.getVehicle() != null) {  // vehicles to move
					train = moveVehicles(time, task);
				}
				book = op.decision().processDropoff(time, task);
				event = new CarsharingDropoffVehicleEvent(
								task.getTime(), qSim.getScenario(), 
								m, task.getAgent(), task.getStation(), 
								train, task.getBooking());
			}
			
			if(book) { 
				this.m.booking().track(task.getStation()).confirm(task.getBooking(), train.peek());
			}
			
			qSim.getEventsManager().processEvent(event);
								
		}
	}
	
	
	private Queue<CarsharingVehicleMobsim> moveVehicles(final double time, CarsharingRelocationTask task) {
		CarsharingOperatorMobsim op = (CarsharingOperatorMobsim) task.getAgent();
		Queue<CarsharingVehicleMobsim> rt = op.getVehicle().roadTrain();
		double distance = task.getDistance();
		double avgspeed = task.getDistance()/task.getTravelTime();
		double maxspeed = task.getDistance()/task.getTravelTime();
		//double maxspeed = op.getVehicle().vehicle().getType().getMaximumVelocity();
		op.getVehicle().drive(distance, avgspeed, maxspeed);
		for(CarsharingVehicleMobsim v : rt) { // teleport
			logger.info("[DRIVING] T:" + (int)time + 
					" |taskId:"+task.getId()+ 
					" |vehId"+v.vehicle().getId()+ 
					" |soc:" + v.battery().getSoC() + 
					" |dist:" + distance + 
					" |tt:" + task.getTravelTime() + 
					" |avgspd:" + avgspeed + 
					" |maxspd:" + maxspeed);
			qSim.createAndParkVehicleOnLink(v.vehicle(), task.getStation().facility().getLinkId());
		}
		return rt;
	}

	
}
