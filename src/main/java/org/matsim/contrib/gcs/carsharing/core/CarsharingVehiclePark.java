package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehiclePark;

public class CarsharingVehiclePark {

	private final CarsharingVehicleMobsim vehicle;
	private final CarsharingStationMobsim station;	
	
	private double vehicleDropoffTime;
	private double vehicleDropoffSoC;
	private double vehiclePickupTime;
	private double energyCharged;
	private double chargingDuration;
	private boolean pickedAsTrailer;
	
	private double endChargingTime;
	
	private String id;

	/**
	 * 
	 * @param station
	 * @param vehicleDropoffTime
	 */
	public CarsharingVehiclePark(CarsharingVehicleMobsim vehicle, String idperson, CarsharingStationMobsim station, double vehicleDropoffTime) {
		this.vehicle = vehicle;
		this.station = station;
		this.vehicleDropoffTime = vehicleDropoffTime;
		this.vehicleDropoffSoC = this.vehicle.battery().getSoC();
		this.vehiclePickupTime = Double.NaN; // Not had been picked up yet
		this.energyCharged = Double.NaN; // Not had been picked up yet
		this.chargingDuration = Double.NaN;
		this.pickedAsTrailer = false;
		this.endChargingTime = Double.NaN;
		this.id = idperson + "@" + String.valueOf(vehicleDropoffTime);
	}
			
	
	/**
	 * 
	 * @param driveVehicle
	 * @param vehiclePickupTime
	 * @param vehicleChargingDuration
	 */
	public void finalize(boolean asTrailer, double vehiclePickupTime) {
		this.vehiclePickupTime = vehiclePickupTime;
		this.energyCharged = this.vehicle.battery().getSoC() - this.vehicleDropoffSoC;
		this.pickedAsTrailer = asTrailer;
	}
	
	/**
	 * 
	 * @param vehicleChargingDuration
	 */
	public void incrementChargingDuration(double vehicleChargingDuration) {
		this.chargingDuration += vehicleChargingDuration;
	}
	

	public void setEndChargingTime(double endChargingTime) {
		this.endChargingTime = endChargingTime;
	}
	
	public double getVehicleDropoffTime() { return this.vehicleDropoffTime; }
	public double getVehicleDropoffSoC() { return this.vehicleDropoffSoC; }
	public double getVehiclePickupTime() { return this.vehiclePickupTime; }
	public double getEnergyCharged() { return this.energyCharged; }
	public double getChargingDuration() { return this.chargingDuration; }
	public boolean getPickedAsRoadTrain() { return this.pickedAsTrailer; }
	public CarsharingStationMobsim getStation() { return this.station; }
	public CarsharingVehicleMobsim getVehicle() { return this.vehicle; }
	public double getEndChargingTime() { return this.endChargingTime; }
	public String getId() { return this.id; }

}
