package org.matsim.contrib.gcs.carsharing.core;

public class CarsharingBookingRecord {

		double bookingTime;
		CarsharingAgent person;
		CarsharingStationMobsim sourceStation;
		double departureTime;
		int numberOfVehicles;
		CarsharingStationMobsim destinationStation;
		double arrivalTime;
		String trip;
		String park;
		CarsharingVehicleMobsim vehicle;
		CarsharingOffer relatedOffer;
		CarsharingDemand demand;
		
		boolean noVehicleOffer; 
		boolean noParkingOffer;
		boolean betterWalk;
		String id;
		String comment = null;
		
		CarsharingBookingRecord() {}
		
		static CarsharingBookingRecord init() {
			CarsharingBookingRecord b = new CarsharingBookingRecord();
			if(b.betterWalk()) {
				b.comment = "WALK";
			} else if(!b.vehicleOffer() || !b.parkingOffer()) {
				b.comment = "PT";
			} else {
				b.comment = "CS";
			}
			return b;
		}
		
		public static CarsharingBookingRecord constructAndGetFailedBookingRec(
				double bookingTime, 
				CarsharingDemand demand, 
				boolean betterWalk,
				boolean noVehicleOffer, CarsharingStationMobsim depStation, double depTime,
				boolean noParkingOffer,	CarsharingStationMobsim arrStation,	double arrTime) {
			CarsharingBookingRecord b = new CarsharingBookingRecord();
			b.bookingTime = bookingTime;
			b.demand = demand;
			b.person = demand.getAgent();
			b.trip = null;
			b.park = null;
			b.noVehicleOffer = noVehicleOffer;
			b.sourceStation = depStation;
			b.departureTime = depTime;
			b.noParkingOffer = noParkingOffer;
			b.destinationStation = arrStation;
			b.arrivalTime = arrTime;
			b.id = ((b.person == null)?"NA":b.person.getId()) + "@" + (int)bookingTime + "-FAILED";
			b.betterWalk = betterWalk;
			if(b.betterWalk()) {
				b.comment = "WALK";
			} else if(!b.vehicleOffer() || !b.parkingOffer()) {
				b.comment = "PT";
			} else {
				b.comment = "CS";
			}
			return b;
		}
		
		public static CarsharingBookingRecord constructAndGetFailedBookingRec(double bookingTime, CarsharingOffer offer) {
			return constructAndGetFailedBookingRec(bookingTime, offer.getDemand(), false, false, 
					offer.getAccess().getStation(), offer.getDepartureTime(),
					false, offer.getEgress().getStation(), offer.getArrivalTime());
		}
		
		public static CarsharingBookingRecord constructAndGetBookingRec(double bookingTime, CarsharingOffer offer) {
			CarsharingBookingRecord b = constructAndGetFailedBookingRec(bookingTime, offer.getDemand(), false, false, 
					offer.getAccess().getStation(), offer.getDepartureTime(),
					false, offer.getEgress().getStation(), offer.getArrivalTime());
			b.id = ((b.person == null)?"NA":b.person.getId()) + "-" + b.sourceStation.getId() + "@" + (int)bookingTime;
			return b;
		}
		

						
		public void setVehicle(CarsharingVehicleMobsim v) { this.vehicle = v; }
		public void setArrivalTime(double arrivalTime) { this.arrivalTime = arrivalTime; }
		public void setTrip(String tripLabel) { this.trip = tripLabel; }
		public void setPark(String parkId) { this.park = parkId; }
		public void setDestinationStation(CarsharingStationMobsim stationDestination) {	this.destinationStation = stationDestination; }
		public void setOriginStation(CarsharingStationMobsim sourceStation) { this.sourceStation = sourceStation; }
		public void setDepartureTime(double departureTime) { this.departureTime = departureTime; }
		public void setNbrOfVeh(int nbreveh) { this.numberOfVehicles = nbreveh; }
		public void setRelatedOffer(CarsharingOffer offer) { this.relatedOffer = offer; }
		public void setAgent(CarsharingAgent a) { this.person = a; }
		public void setComment(String s) { this.comment = s; }
		
		public String getComment() { return this.comment; }
		public String getId() {	return this.id;	}
		public double getDepartureTime() { return this.departureTime; }
		public double getArrivalTime() { return this.arrivalTime; }
		public int getNbrOfVeh() { return this.numberOfVehicles; }
		public CarsharingStationMobsim getOriginStation() { return this.sourceStation; }
		public CarsharingStationMobsim getDestinationStation() { return this.destinationStation; }
		public double getBookingTime() { return this.bookingTime; }
		public CarsharingOffer getRelatedOffer() { return this.relatedOffer; }
		public CarsharingDemand getDemand() { return this.demand; }
		public CarsharingVehicleMobsim getVehicle() { return this.vehicle; }
		public CarsharingAgent getAgent() { return this.person; }
		
		
		public String trip() { return this.trip; }
		public String park() { return this.park; }

		public boolean vehicleOffer() { return !this.noVehicleOffer; }
		public boolean parkingOffer() { return !this.noParkingOffer; }
		
		public boolean betterWalk() { return this.betterWalk; }
		public boolean bookingFailed() { return this.noVehicleOffer || this.noParkingOffer; }
		
}
