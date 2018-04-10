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
		
		Boolean vehicleOffer; 
		Boolean parkingOffer;
		String id;
		
		CarsharingBookingRecord() {}
		
		public static CarsharingBookingRecord constructBookingRec(
				double bookingTime, CarsharingDemand demand, 
				Boolean vehicleOffer, CarsharingStationMobsim depStation, double depTime,
				Boolean parkingOffer,	CarsharingStationMobsim arrStation,	double arrTime) {
			CarsharingBookingRecord b = new CarsharingBookingRecord();
			b.bookingTime = bookingTime;
			b.demand = demand;
			if(demand != null)	b.person = demand.getAgent();
			else b.person = null;
			b.trip = null;
			b.park = null;
			b.vehicleOffer = vehicleOffer;
			b.sourceStation = depStation;
			b.departureTime = depTime;
			b.parkingOffer = parkingOffer;
			b.destinationStation = arrStation;
			b.arrivalTime = arrTime;
			b.id = ((b.person == null)?"NA":b.person.getId()) + "@" + (int)bookingTime;
			return b;
		}
		
		public static CarsharingBookingRecord constructAndGetBookingRec(double bookingTime, CarsharingOffer offer) {
			Boolean nv = null, np = null;
			if(offer.getAccess().getStation() != null) {
				nv = offer.getAccess().getStatus().isValid();
			}
			if(offer.getEgress().getStation() != null) {
				np = offer.getEgress().getStatus().isValid();
			}
			CarsharingBookingRecord b = constructBookingRec(bookingTime, offer.getDemand(), 
					nv, offer.getAccess().getStation(), offer.getDepartureTime(),
					np, offer.getEgress().getStation(), offer.getArrivalTime());
			b.relatedOffer = offer;
			b.numberOfVehicles = offer.getNbOfVehicles();
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

		public Boolean vehicleOffer() { return this.vehicleOffer!=null && this.vehicleOffer; }
		public Boolean parkingOffer() { return this.parkingOffer!=null && this.parkingOffer; }
		
		public boolean bookingFailed() { return vehicleOffer() == false || parkingOffer() == false; }
		
}
