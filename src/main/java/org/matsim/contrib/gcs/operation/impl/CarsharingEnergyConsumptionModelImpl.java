/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.gcs.operation.impl;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.gcs.operation.model.CarsharingEnergyConsumptionModel;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;

// EnergyConsumptionModelImpl
// EnergyConsumptionModelRicardoFaria2012

public class CarsharingEnergyConsumptionModelImpl implements CarsharingEnergyConsumptionModel {

	protected PriorityQueue<EnergyConsumption> queue = new PriorityQueue<EnergyConsumption>();
	private EnergyConsumption zeroSpeedConsumption = new EnergyConsumption(0, 0);
	
	
	
	public CarsharingEnergyConsumptionModelImpl() {
		queue.add(new EnergyConsumption(5.555555556, 3.19E+02));
		queue.add(new EnergyConsumption(8.333333333, 3.10E+02));
		queue.add(new EnergyConsumption(11.11111111, 3.29E+02));
		queue.add(new EnergyConsumption(13.88888889, 3.56E+02));
		queue.add(new EnergyConsumption(16.66666667, 4.14E+02));
		queue.add(new EnergyConsumption(19.44444444, 4.50E+02));
		queue.add(new EnergyConsumption(22.22222222, 5.13E+02));
		queue.add(new EnergyConsumption(25, 5.85E+02));
		queue.add(new EnergyConsumption(27.77777778, 6.62E+02));
		queue.add(new EnergyConsumption(30.55555556, 7.52E+02));
		queue.add(new EnergyConsumption(33.33333333, 8.46E+02));
	}
	
	
	/**
	 * 
	 * Gives the interpolated energy consumption. Speed must be inside the
	 * original interval (borders included).
	 * 
	 * @param consumptionA
	 * @param consumptionB
	 * @param speed
	 * @return
	 */
	protected static double getInterpolatedEnergyConsumption(EnergyConsumption consumptionA, EnergyConsumption consumptionB,
			double speed) {
		EnergyConsumption smallerSpeedEC;
		EnergyConsumption biggerSpeedEC;

		if (consumptionA.getSpeed() < consumptionB.getSpeed()) {
			smallerSpeedEC = consumptionA;
			biggerSpeedEC = consumptionB;
		} else if (consumptionA.getSpeed() < consumptionB.getSpeed()) {
			smallerSpeedEC = consumptionB;
			biggerSpeedEC = consumptionA;
		} else {
			return (consumptionA.getEnergyConsumption() + consumptionB.getEnergyConsumption()) / 2;
		}

		if (speed < smallerSpeedEC.getSpeed() || speed > biggerSpeedEC.getSpeed()) {
			DebugLib.stopSystemAndReportInconsistency("input speed is not inside given interval");
		}

		if (speed == smallerSpeedEC.getSpeed()) {
			return smallerSpeedEC.getEnergyConsumption();
		}

		if (speed == biggerSpeedEC.getSpeed()) {
			return biggerSpeedEC.getEnergyConsumption();
		}

		double differenceSpeed = biggerSpeedEC.getSpeed() - smallerSpeedEC.getSpeed();
		double differenceEnergyConsumption = biggerSpeedEC.getEnergyConsumption() - smallerSpeedEC.getEnergyConsumption();

		double interpolationFactor = differenceEnergyConsumption / differenceSpeed;

		double result = smallerSpeedEC.getEnergyConsumption() + interpolationFactor * (speed - smallerSpeedEC.getSpeed());
		return result;
	}
	
	/**
	 * @param speedInMetersPerSecond
	 * @param distanceInMeters
	 * @return
	 */
	protected double getInterpolatedEnergyConsumption(double speedInMetersPerSecond, double distanceInMeters) {
		Iterator<EnergyConsumption> iter = queue.iterator();

		EnergyConsumption currentAverageConsumption = iter.next();

		if (ifSmallerThanMimimumSpeed(speedInMetersPerSecond, currentAverageConsumption)) {
			return currentAverageConsumption.getEnergyConsumption() * distanceInMeters;
		}
		EnergyConsumption previousConsumption = null;

		while (currentAverageConsumption.getSpeed() < speedInMetersPerSecond && iter.hasNext()) {
			previousConsumption = currentAverageConsumption;
			currentAverageConsumption = iter.next();
		}

		if (ifHigherThanMaxSpeed(speedInMetersPerSecond, currentAverageConsumption)) {
			return currentAverageConsumption.getEnergyConsumption() * distanceInMeters;
		} else {
			return getInterpolatedEnergyConsumption(previousConsumption, currentAverageConsumption, speedInMetersPerSecond)
					* distanceInMeters;
		}
	}
	
	private boolean ifHigherThanMaxSpeed(double speedInMetersPerSecond, EnergyConsumption currentAverageConsumption) {
		return currentAverageConsumption.getSpeed() < speedInMetersPerSecond;
	}

	private boolean ifSmallerThanMimimumSpeed(double speedInMetersPerSecond, EnergyConsumption currentAverageConsumption) {
		return currentAverageConsumption.getSpeed() >= speedInMetersPerSecond;
	}
	
	@Override
	public double getEnergyConsumptionForLinkInJoule(Link link, double averageSpeedDriven) {
		return getEnergyConsumptionForLinkInJoule(link.getLength(), link.getFreespeed(), averageSpeedDriven);
	}

	@Override
	public double getEnergyConsumptionForLinkInJoule(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven) {
		return getInterpolatedEnergyConsumption(averageSpeedDriven, drivenDistanceInMeters);
	}
	
	
	public class EnergyConsumption implements Comparable<EnergyConsumption> {
		private double speedInMeterPerSecond=0;
		private double energyConsumptionInJoule=0; // consumed energy in [J] (by driving one meter with the given speed)
		
		public EnergyConsumption(double speed, double energyConsumption){
			this.speedInMeterPerSecond=speed;
			this.energyConsumptionInJoule=energyConsumption;
		}
		
		public double getSpeedDifference(double otherSpeed){
			return Math.abs(speedInMeterPerSecond-otherSpeed);
		}
		
		public double getEnergyConsumption(){
			return energyConsumptionInJoule;
		}
		
		public double getSpeed(){
			return speedInMeterPerSecond;
		}

		public int compareTo(EnergyConsumption otherConsumption) {
			if (this.speedInMeterPerSecond<otherConsumption.speedInMeterPerSecond){
				return -1;
			} else if (this.speedInMeterPerSecond>otherConsumption.speedInMeterPerSecond){
				return 1;
			}
			return 0;
		}
	}
}
