package org.matsim.haitam.api.carsharing.core;

public class CarsharingVehicleTrip {

	public static final String STATUS_ENDTRIP_OK = "END";
	public static final String STATUS_ENDTRIP_NOSPACE = "NOSPACE";
	public static final String STATUS_ENDTRIP_NOENERGY = "NOENERGY";
	
	private final CarsharingVehicleMobsim vehicle;
	private final CarsharingStationMobsim sourceStation;
	private CarsharingStationMobsim destinationStation;
	private double departureTime;
	private double departureSoC;
	private double travelDistance;
	private double rentalDuration;
	private double energyConsumed;
	private double rentalCost;
	private String status;
	private boolean asTrailer;
	private String id;
	
	/**
	 * 
	 * @param person
	 * @param sourceStation
	 * @param departureTime
	 */
	public CarsharingVehicleTrip(CarsharingVehicleMobsim vehicle, String idperson, CarsharingStationMobsim sourceStation, double departureTime, boolean asTrailer) {
		this.vehicle = vehicle;
		this.sourceStation = sourceStation;
		this.departureTime = departureTime;
		this.departureSoC = this.vehicle.battery().getSoC();						
		this.destinationStation = null;
		this.travelDistance = Double.NaN;
		this.rentalDuration = Double.NaN;
		this.energyConsumed = Double.NaN;
		this.asTrailer = asTrailer;
		this.status = "START";
		this.id = idperson + "@" + String.valueOf((int)departureTime); 
	}
	
	/**
	 * 
	 * @param destinationStation
	 * @param travelDistance
	 * @param rentalDuration
	 * @param energyConsumed
	 * @param rentalCost
	 */
	public void finalize(double arrivalTime, CarsharingStationMobsim destinationStation, String tripStatus) {
		this.destinationStation = destinationStation;
		this.rentalDuration = arrivalTime - this.departureTime;
		this.status = tripStatus;
	}
	
	/**
	 * 
	 * @param travelDistance
	 */
	public void increment(double travelDistance, double energyConsumed) {
		if(Double.isNaN(this.travelDistance)) {	this.travelDistance = 0; }
		if(Double.isNaN(this.energyConsumed)) { this.energyConsumed = 0; }
		this.travelDistance += travelDistance;
		this.energyConsumed -= energyConsumed;
	}
	
	public void setDepartureTime(double time) { this.departureTime = time; }
	public void setRentalCost(Double cost) { this.rentalCost = cost; }
	
	public String getId() { return this.id; }
	public String getSourceStationName() { return sourceStation.getName(); }
	public CarsharingStationMobsim getSourceStation() { return sourceStation; }
	public double getDepartureTime() { return departureTime; }
	public double getDepartureSoC() { return departureSoC; }
	public String getDestinationStationName() { return destinationStation.getName(); }
	public CarsharingStationMobsim getDestinationStation() { return destinationStation; }
	public double getTravelDistance() { return travelDistance; }
	public double getRentalDuration() { return rentalDuration; }
	public double getEnergyConsumed() { return energyConsumed; }
	public double getRentalCost() { return rentalCost; }
	public String getStatus() { return status; }
	public CarsharingVehicleMobsim getVehicle() { return this.vehicle; }
	

}
