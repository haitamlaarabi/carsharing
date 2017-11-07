package org.matsim.haitam.api.carsharing.core;

import org.apache.log4j.Logger;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.MathLib;
import org.matsim.haitam.api.carsharing.core.CarsharingVehicleBattery;
import org.matsim.haitam.api.operation.model.CarsharingBatteryModel;
import org.matsim.haitam.api.operation.model.CarsharingEnergyConsumptionModel;

public class CarsharingVehicleBattery {

	private static Logger logger = Logger.getLogger(CarsharingVehicleBattery.class);
	
	private final CarsharingEnergyConsumptionModel consumption;
	private final CarsharingBatteryModel battery;
	private double SoC;
	
	public CarsharingVehicleBattery(CarsharingBatteryModel battery, CarsharingEnergyConsumptionModel consumption) {
		this.battery = battery;
		this.consumption = consumption;
		this.SoC = 0;
	}
	
	public double getSoC() { return this.SoC; }

	public double getEnergyForConsumption(double drivenDistanceInMeters, double maxSpeedOnLink,	double averageSpeedDriven) {
		return consumption.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink, averageSpeedDriven);
	}

	public double getEnergyForCharging(double powerInJoules, double durationInSeconds) {
		return battery.calculateChargingRate(getSoC(), powerInJoules, durationInSeconds);
	}

	public double getEnergyForBalancing(double durationInSeconds) {
		if(durationInSeconds == Double.NaN) {
			return battery.getBalancingEnergy(getSoC());
		}
		return battery.getBalancingEnergy(getSoC(), durationInSeconds);
	}

	public double getPowerForBalancing() {
		return battery.getBalancingPower();
	}

	public double getLowBatteryThreshold() {
		return battery.calculateCriticalSocThreshold();
	}

	public double getInitialStateOfCharge() {
		return battery.calculateInitialSoc();
	}

	public double getSafeBatteryCapacity() {
		return battery.safeCapacity();
	}

	public double getTheoreticalBatteryCapacity() {
		return battery.theoreticalCapacity();
	}

	public double getRequiredEnergy() {
		double required = getSafeBatteryCapacity() - getSoC();
		if(required < 0)
			throw new RuntimeException("getSafeBatteryCapacity() - getSoC() < 0");
		return required;
	}

	public CarsharingEnergyConsumptionModel getEnergyConsumptionModel() {
		return consumption;
	}

	public CarsharingBatteryModel getBatteryModel() {
		return battery;
	}

	public boolean isDead() {
		return (getSoC() < 0);
	}

	public boolean batteryIsRunningLow() {
		return (getSoC() < getLowBatteryThreshold());
	}

	public boolean isFullyCharged() {
		return (getSoC() >= getSafeBatteryCapacity());
	}

	public double chargeBattery(double power, double duration) {
		double energyCharged = getEnergyForCharging(power, duration);
		chargeBattery(energyCharged);
		return energyCharged;
	}

	public void chargeBattery(double energy) {
		incrementSoC(+1 * energy);
		if (!MathLib.equals(getSoC(), getSafeBatteryCapacity(), GeneralLib.EPSILON * 100)
				&& getSoC() > getSafeBatteryCapacity()) {
			DebugLib.stopSystemAndReportInconsistency("the car has been overcharged" + getSoC() + " but MC"
					+ getSafeBatteryCapacity());
		}
	}

	public double consumeBattery(double distance, double maxspeed, double averageSpeed) {
		double energyConsumed = getEnergyForConsumption(distance, maxspeed, averageSpeed);
		incrementSoC(-1 * energyConsumed);
		if (!MathLib.equals(
				getSoC(), 0, GeneralLib.EPSILON * 100) && getSoC() < 0) {
			//DebugLib.stopSystemAndReportInconsistency("SoC is negative " + getSoC());
			logger.error("SoC is negative " + getSoC());
		}
		return energyConsumed;
	}
	
	public boolean isChargedEnough(double distance, double maxspeed, double averageSpeed) {
		double energyConsumed = getEnergyForConsumption(distance, maxspeed, averageSpeed);
		double estimatedSoc = this.SoC - energyConsumed;
		if (!MathLib.equals(
				estimatedSoc, 0, GeneralLib.EPSILON * 100) && estimatedSoc < 0) {
			return false;
		}
		return true;
	}

	public void drainBattery(double energy) {
		incrementSoC(-1 * energy);
		if (!MathLib.equals(
				getSoC(), 0, GeneralLib.EPSILON * 100) && getSoC() < 0) {
			DebugLib.stopSystemAndReportInconsistency("SoC is negative " + getSoC());
		}
	}

	public void incrementSoC(double SoC) {
		this.SoC += SoC;
	}

	public void reset() {
		this.SoC = 0;
		incrementSoC(battery.calculateInitialSoc());
	}

}
