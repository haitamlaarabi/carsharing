package org.matsim.contrib.gcs.carsharing.core;

import org.apache.log4j.Logger;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleBattery;
import org.matsim.contrib.gcs.operation.model.CarsharingBatteryModel;
import org.matsim.contrib.gcs.operation.model.CarsharingEnergyConsumptionModel;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.MathLib;

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

	public double energyConsumptionQty(double speedInMetersPerSecond, double distanceInMeters) {
		return consumption.calculateEnergyConsumptionInJoule(speedInMetersPerSecond, distanceInMeters);
	}
	
	public double energySupplyQty(double powerInJoules, double durationInSeconds) {
		return battery.calculateChargingRateInJoule(getSoC(), powerInJoules, durationInSeconds);
	}

	public double energyBalancingQty(double durationInSeconds) {
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
		double energyCharged = energySupplyQty(power, duration);
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

	public double consumeBattery(double speedInMetersPerSecond, double distanceInMeters) {
		double energyConsumed = energyConsumptionQty(speedInMetersPerSecond, distanceInMeters);
		incrementSoC(-1 * energyConsumed);
		if (!MathLib.equals(
				getSoC(), 0, GeneralLib.EPSILON * 100) && getSoC() < 0) {
			//DebugLib.stopSystemAndReportInconsistency("SoC is negative " + getSoC());
			logger.error("SoC is negative " + getSoC());
		}
		return energyConsumed;
	}
	
	public boolean checkBattery(double speedInMetersPerSecond, double distanceInMeters) {
		double energyConsumed = energyConsumptionQty(speedInMetersPerSecond, distanceInMeters);
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
