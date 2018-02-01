package org.matsim.contrib.gcs.carsharing.core;

import java.util.Queue;

public interface CarsharingVehicleMobsim extends CarsharingVehicle {

	Queue<CarsharingVehicleMobsim> roadTrain();
	CarsharingVehicleBattery battery();
	void reset(int iteration);
	void dock(CarsharingVehicleMobsim vehicle);
	Queue<CarsharingVehicleMobsim> undock();
	void startTrip(CarsharingAgent agent, CarsharingStationMobsim station, double time);
	void endTrip(CarsharingStationMobsim arrivalStation, double time, String tripstatus);
	void startPark(CarsharingAgent agent, CarsharingStationMobsim station, double time);
	void endPark(double time);
	CarsharingVehicleStatus status();
	void drive(double traveltime, double traveldistance);
	
	public static class CarsharingVehicleStatus {
		String type;
		CarsharingVehicleTrip trip;
		CarsharingVehiclePark park;
		public String getType() { return type; }
		public CarsharingVehicleTrip getTrip() { return trip; }
		public CarsharingVehiclePark getPark() { return park; }
		public void setType(String type) { this.type = type; }
		public void setTrip(CarsharingVehicleTrip t) { 
			this.trip = t;
			this.setType("INUSE");
		}
		public void setPark(CarsharingVehiclePark p) { 
			this.park = p; 
			this.setType("PARKED");
		}
	}
	
}
