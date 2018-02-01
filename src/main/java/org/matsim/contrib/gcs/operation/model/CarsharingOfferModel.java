package org.matsim.contrib.gcs.operation.model;

import java.util.ArrayList;

import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingOfferModel extends CarsharingOperationModel {	
	
	public double computeRentalCost(CarsharingOffer offer, double rentalduration);
	public ArrayList<CarsharingOffer> computeRentalOffers(int time, CarsharingDemand demand);
	
}
