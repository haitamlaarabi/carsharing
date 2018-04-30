package org.matsim.contrib.gcs.carsharing.core;

public class CarsharingBookingRecord {

		int bookingTime;
		CarsharingAgent person;
		CarsharingStationMobsim sourceStation;
		int departureTime;
		int numberOfVehicles;
		CarsharingStationMobsim destinationStation;
		int arrivalTime;
		String trip;
		String park;
		CarsharingVehicleMobsim vehicle;
		CarsharingOffer relatedOffer;
		CarsharingDemand demand;
		
		private Boolean vehicleOffer; 
		private Boolean parkingOffer;
		String id;
		
		CarsharingBookingRecord() {}
		
		public static CarsharingBookingRecord constructBookingRec(
				int bookingTime, CarsharingDemand demand, 
				Boolean vehicleOffer, CarsharingStationMobsim depStation, int depTime,
				Boolean parkingOffer,	CarsharingStationMobsim arrStation,	int arrTime) {
			CarsharingBookingRecord b = new CarsharingBookingRecord();
			b.person = null;
			b.trip = null;
			b.park = null;
			b.bookingTime = bookingTime;
			b.demand = demand;
			b.trip = null;
			b.park = null;
			b.vehicleOffer = vehicleOffer;
			b.sourceStation = depStation;
			b.departureTime = depTime;
			b.parkingOffer = parkingOffer;
			b.destinationStation = arrStation;
			b.arrivalTime = arrTime;
			if(demand != null)	b.person = demand.getAgent();
			b.id = ((b.person == null)?"NA":b.person.getId()) + "@" + (int)bookingTime;
			return b;
		}
		
		public static CarsharingBookingRecord constructAndGetBookingRec(int bookingTime, CarsharingOffer offer) {
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
			b.person = offer.getAgent();
			b.numberOfVehicles = offer.getNbOfVehicles();
			b.id = b.person.getId() + "@" + (int)bookingTime;
			return b;
		}
		

						
		public void setVehicle(CarsharingVehicleMobsim v) { this.vehicle = v; }
		public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }
		public void setTrip(String tripLabel) { this.trip = tripLabel; }
		public void setPark(String parkId) { this.park = parkId; }
		public void setDestinationStation(CarsharingStationMobsim stationDestination) {	this.destinationStation = stationDestination; }
		public void setOriginStation(CarsharingStationMobsim sourceStation) { this.sourceStation = sourceStation; }
		public void setDepartureTime(int departureTime) { this.departureTime = departureTime; }
		public void setNbrOfVeh(int nbreveh) { this.numberOfVehicles = nbreveh; }
		public void setRelatedOffer(CarsharingOffer offer) { this.relatedOffer = offer; }
		public void setAgent(CarsharingAgent a) { this.person = a; }
		
		public String getId() {	return this.id;	}
		public int getDepartureTime() { return this.departureTime; }
		public int getArrivalTime() { return this.arrivalTime; }
		public int getNbrOfVeh() { return this.numberOfVehicles; }
		public CarsharingStationMobsim getOriginStation() { return this.sourceStation; }
		public CarsharingStationMobsim getDestinationStation() { return this.destinationStation; }
		public int getBookingTime() { return this.bookingTime; }
		public CarsharingOffer getRelatedOffer() { return this.relatedOffer; }
		public CarsharingDemand getDemand() { return this.demand; }
		public CarsharingVehicleMobsim getVehicle() { return this.vehicle; }
		public CarsharingAgent getAgent() { return this.person; }
		
		public String trip() { return this.trip; }
		public String park() { return this.park; }

		public Boolean vehicleOffer() { return this.vehicleOffer!=null && this.vehicleOffer; }
		public Boolean parkingOffer() { return this.parkingOffer!=null && this.parkingOffer; }
		
		public void setVehicleOffer(Boolean vo) {
			this.vehicleOffer = vo;
		}
		
		public void setParkingOffer(Boolean po) {
			this.parkingOffer = po;
		}
		
		public boolean bookingFailed() { return vehicleOffer() == false || parkingOffer() == false; }
		
}
