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
			Queue<CarsharingVehicleMobsim> roadTrain = null;
			op.setLocation(task.getStation()); // move to station
			if(task.getSize() == 0) {
				// do nothing
			} else {
				boolean flag =false;
				if(task.getType().equals("START")) { 
					flag = op.decision().processPickup(time, task);
					if(op.getVehicle() != null) { roadTrain = op.getVehicle().roadTrain(); } // pick-up
				}  else {
					if(op.getVehicle() != null) { 
						roadTrain = op.getVehicle().roadTrain();
						double distance = task.getDistance();
						double avgspeed = task.getDistance()/task.getTravelTime();
						double maxspeed = op.getVehicle().vehicle().getType().getMaximumVelocity();
						op.getVehicle().drive(distance, avgspeed, maxspeed);
						for(CarsharingVehicleMobsim v : roadTrain) { // teleport
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
						flag = op.decision().processDropoff(time, task);
					}
					if(flag)
						this.m.booking().track(task.getStation()).confirm(task.getBooking(), roadTrain.peek());
				}
			}
			op.endTask();
			
			if(task.getType().equals("START")) { 
				qSim.getEventsManager().processEvent(
						new CarsharingPickupVehicleEvent(
								task.getTime(), 
								qSim.getScenario(), 
								this.m, 
								task.getAgent(), 
								task.getStation(), 
								roadTrain,
								task.getBooking()));
			} else {
				qSim.getEventsManager().processEvent(
						new CarsharingDropoffVehicleEvent(
								task.getTime(), 
								qSim.getScenario(), 
								this.m, 
								task.getAgent(),
								task.getStation(), 
								roadTrain,
								task.getBooking()));
			}
					
		}
	}
	

	
}
