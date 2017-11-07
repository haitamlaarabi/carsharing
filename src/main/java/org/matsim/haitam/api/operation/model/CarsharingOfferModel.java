package org.matsim.haitam.api.operation.model;

import java.util.ArrayList;

import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingOfferModel extends CarsharingOperationModel {	
	
	public double computeRentalCost(CarsharingOffer offer, double rentalduration);
	public ArrayList<CarsharingOffer> computeRentalOffers(double time, CarsharingDemand demand);
	
}
