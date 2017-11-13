package org.matsim.contrib.gcs.replanning;

public final class CarsharingPlanModeCst {

	/**
	 * 
	 */
	public static final String ongoingTrip = "carsharingOngoingTrip";
	public static final Integer ongoingTrip_Size = 3;
	
	public static final String startTrip = "carsharingStartTrip";
	public static final Integer startTrip_Size = 4;
	
	public static final String endTrip = "carsharingEndTrip";
	public static final Integer endTrip_Size = 4;
	
	
	/**
	 * 
	 */
	public static final String directTrip = "carsharingDirectTrip";
	public static final Integer directTrip_Size = 5;
	
	
	
	/*public static boolean check(String mode) {
		return 	mode.equals(ongoingTrip) ||
				mode.equals(startTrip) ||
				mode.equals(endTrip) ||
				mode.equals(directTrip);
	}*/
}
