package org.matsim.contrib.gcs.operation.impl;

import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingAgent;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleTrip;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;

public class CarsharingOperatorChoiceModelImpl implements CarsharingOperatorChoiceModel {

	private static Logger logger = Logger.getLogger(CarsharingOperatorChoiceModelImpl.class);
	
	CarsharingOperatorMobsim op;
	CarsharingManager m;
	boolean canpickup;
	
	public CarsharingOperatorChoiceModelImpl(CarsharingManager m) {
		this.m = m;
		this.canpickup = true;
	}
	
	@Override
	public boolean processPickup(double time, CarsharingRelocationTask task) {
		CarsharingStationMobsim here = task.getStation();
		
		if(!task.getStation().equals(op.getLocation())) {
			logger.warn("[R-PU-WRONG-LOCATION] T:" + (int)time + " |tId:"+task.getId()+" |staId:"+task.getStation().getId()+" |locationId:"+op.getLocation().getId()+" |agentId:"+task.getAgent().getId());
		} else {
			if(this.canpickup) { // if agent can pick up and there are vehicles to pick up
				if(!ENERGY(task, time)) {
					logger.warn("[RPickupENERGY-KO] T:" + (int)time + "|tId:"+task.getId()+"|staId:"+task.getStation().getId()+"|linkId:"+task.getStation().facility().getLinkId()+"|agentId:"+task.getAgent().getId());
				} else {
					int rt_size = Math.min(this.m.booking().track(here).vehicleAvailability(), task.getSize());
					this.op.setVehicle(here.pickup(this.op, rt_size, time)); // pickup
					if(this.op.getVehicle() != null) { 
						this.canpickup = false;
						return true;
					} else if(task.getSize() != 0) {
						logger.warn("[R-PU-KO] T:" + (int)time + " |tId:"+task.getId()+" |staId:"+task.getStation().getId()+" |linkId:"+task.getStation().facility().getLinkId()+" |agentId:"+task.getAgent().getId());
					}
				}
			} else if(this.op.getVehicle() != null) { // otherwise, if operator already have vehicles
				this.op.getVehicle().startTrip(this.op, this.op.getLocation(), time); // start a new trip
				this.canpickup = false;
			}
		}
		op.endTask();
		return false;
	}
	
	@Override
	public boolean processDropoff(double time, CarsharingRelocationTask task) {
		CarsharingStationMobsim here = task.getStation();
		CarsharingVehicleMobsim VEH = this.op.getVehicle();
		
		if(task.getStation().equals(op.getLocation())) {
			logger.warn("[R-DO-SAME-LOCATION] T:" + (int)time + " |tId:"+task.getId()+" |staId:"+task.getStation().getId()+" |locationId:"+op.getLocation().getId()+" |agentId:"+task.getAgent().getId());
		} else {
			if(task.getSize() > 0 && VEH != null) {
				this.canpickup = true;
				Queue<CarsharingVehicleMobsim> q = VEH.roadTrain();
				CarsharingVehicleTrip trip = VEH.status().getTrip();
				if(here.dropoff(this.op, VEH, time)) {
					this.op.setVehicle(null);
					this.canpickup = true;
					return true;
				} else {
					this.canpickup = false;
					for(CarsharingVehicleMobsim v : q) {
						logger.error("[R-DO-KO] T:" + (int)time + 
								" |vehId:"+v.vehicle().getId() + 
								" |tId:"+task.getId()+
								" |staId:"+task.getStation().getId()+
								" |linkId:"+task.getStation().facility().getLinkId()+
								" |agentId:"+task.getAgent().getId() + 
								" |status:"+trip.getStatus()+
								" |SoC:"+v.battery().getSoC());
					}
				}
			}
			this.op.setLocation(here);
		}
		op.endTask();
		return false;
	}

	@Override
	public void bindTo(CarsharingOperatorMobsim user) {
		this.op = user;
	}

	private boolean ENERGY(CarsharingRelocationTask task, double time) {
		CarsharingAgent agent = task.getAgent();
		CarsharingStationMobsim s = task.getStation();
		double distance = task.getDistance();
		int j = task.getSize();
		for(CarsharingVehicleMobsim v : s.parking()) {
			if(j <= 0) break;
			double maxspeed = v.vehicle().getType().getMaximumVelocity();
			double avgspeed = v.vehicle().getType().getMaximumVelocity();
			double eng = v.battery().getEnergyForConsumption(distance, maxspeed, avgspeed);
			double psoc = v.battery().getSoC();
			boolean chargedenough = v.battery().isChargedEnough(distance, maxspeed, avgspeed);
			logger.info(
					"[ENERGY] |T:" + time + 
					" |staId:" + s.getId() + 
					" |agent:" + agent.getId() + 
					" |vehId:" + v.vehicle().getId() + 
					" |soc:" + v.battery().getSoC() + 
					" |maxspeed: " + maxspeed + 
					" |avgspeed: " + avgspeed + 
					" |distance:" + distance + 
					" |consume:" + eng + 
					" |xSoc:" + psoc);
			if(!chargedenough) { return false;}
			j--;
		}
		return true;
	}

}
