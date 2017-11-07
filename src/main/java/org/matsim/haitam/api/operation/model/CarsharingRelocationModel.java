package org.matsim.haitam.api.operation.model;

import java.util.List;

import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.carsharing.core.CarsharingRelocationTask;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingRelocationModel extends CarsharingOperationModel {
	
	
	List<CarsharingOffer> computeUserRelocation(double time, CarsharingDemand demand, List<CarsharingOffer> offers);
	List<CarsharingRelocationTask> computeOperatorRelocation(double time);
	void reset(int iteration);
	
}
