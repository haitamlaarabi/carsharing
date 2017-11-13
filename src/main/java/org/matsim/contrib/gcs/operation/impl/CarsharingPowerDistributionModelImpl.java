package org.matsim.contrib.gcs.operation.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehiclePark;
import org.matsim.contrib.gcs.events.CarsharingChargingEndEvent;
import org.matsim.contrib.gcs.events.CarsharingChargingStartEvent;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerDistributionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerSourceModel;
import org.matsim.core.api.experimental.events.EventsManager;


public class CarsharingPowerDistributionModelImpl implements CarsharingPowerDistributionModel {
	
	CarsharingManager m;
	EventsManager em;
	
	public CarsharingPowerDistributionModelImpl(CarsharingManager m, EventsManager eM) {
		this.m = m;
		this.em = eM;
	}

	@Override
	public void charge(CarsharingParkingModel parkedVehicles, CarsharingPowerSourceModel powerSourceModel, double now) {
		
		// Priority Queue based on SoC level
		PriorityQueue<CarsharingVehicleMobsim> queue = 
				new PriorityQueue<CarsharingVehicleMobsim>(10, 
						new Comparator<CarsharingVehicleMobsim>() {
							@Override
						    public int compare(CarsharingVehicleMobsim x, CarsharingVehicleMobsim y)
						    {
								return Double.compare(x.battery().getSoC(), y.battery().getSoC());
						    }
						});
		
		// Consider only the not fully charged vehicles
		int size = 0;
		for(CarsharingVehicleMobsim vtemp : parkedVehicles) {
			if(!vtemp.battery().isFullyCharged()) 
				queue.add(vtemp);
			size++;
		}
		
		// Uniform Distribution of Power
		while (!queue.isEmpty()) {
			double availablePowerInKiloWatt = (double)powerSourceModel.getPower(now)/size;
			if (availablePowerInKiloWatt == 0) return;
			CarsharingVehicleMobsim vehicleFirst = queue.poll();
			double chargingDuration = chargeFirstInQueue(vehicleFirst, availablePowerInKiloWatt, now);
			if(vehicleFirst.battery().isFullyCharged()) {
				size--;
			}
			
			Iterator<CarsharingVehicleMobsim> itQV = queue.iterator();
			while(itQV.hasNext()){
				CarsharingVehicleMobsim vehicleQueue = itQV.next();	
				// charge remaining vehicles
				chargeQueue(vehicleQueue, availablePowerInKiloWatt, chargingDuration, now);
				if(!vehicleFirst.battery().isFullyCharged()) {
					// If first driveVehicle has not been fully charged, 
					// it means not extra charge for remaining driveVehicle
					// so frop all of them
					itQV.remove();
				} else {
					// IF first driveVehicle has been fully charged in a shorter period
					// we will use extra charge for charging remaining vehicles
					// unless a driveVehicle is fully charged too
					if(vehicleQueue.battery().isFullyCharged()) { 
						size--;
						itQV.remove();
					}
				}
			}
		}		
	}
	
	private double chargeFirstInQueue(CarsharingVehicleMobsim vehicle, double availablePowerInKiloWatt, double now) {
		double energyToChargeInJoules = vehicle.battery().getRequiredEnergy();
		double endChargingTime = vehicle.status().getPark().getEndChargingTime();
		if(Double.isNaN(endChargingTime) || endChargingTime == 0) {
			endChargingTime = vehicle.status().getPark().getVehicleDropoffTime();
		}
		double totalChargingDuration = now - endChargingTime;
		double chargingDuration = totalChargingDuration;
		// TEST IF DURATION IS SUFFICIENT TO FULLY CHARGE OR NOT
		if(availablePowerInKiloWatt * totalChargingDuration >= energyToChargeInJoules) {
			chargingDuration = energyToChargeInJoules/availablePowerInKiloWatt;
		} 
		
		// charge first driveVehicle
		chargeQueue(vehicle, availablePowerInKiloWatt, chargingDuration, now);
		return chargingDuration;
	}
	
	private void chargeQueue(CarsharingVehicleMobsim vehicle, double availablePowerInKiloWatt, double chargingDuration, double now) {
		double endChargingTime = vehicle.status().getPark().getEndChargingTime();
		CarsharingVehiclePark park = vehicle.status().getPark();
		if(Double.isNaN(endChargingTime) || endChargingTime == 0) {
			endChargingTime = park.getVehicleDropoffTime();
			if(chargingDuration > 0) {
				em.processEvent(
						new CarsharingChargingStartEvent(now, m.getScenario(), m, park.getStation(), vehicle.roadTrain(), endChargingTime));	
			}
		}
		vehicle.battery().chargeBattery(availablePowerInKiloWatt, chargingDuration);
		park.incrementChargingDuration(chargingDuration);
		park.setEndChargingTime(endChargingTime + chargingDuration);
		if(vehicle.battery().isFullyCharged()) {
			em.processEvent(
					new CarsharingChargingEndEvent(now,	m.getScenario(), m, park.getStation(), vehicle.roadTrain(), endChargingTime + chargingDuration));	
		}
	}


	@Override
	public String getName() {
		return "uniformlyDitributedPower";
	}

}

