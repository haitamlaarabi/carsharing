package org.matsim.haitam.api.operation.model;

import org.matsim.haitam.api.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingRelocationTask;
import org.matsim.haitam.api.operation.CarsharingOperationModel;

public interface CarsharingOperatorChoiceModel extends CarsharingOperationModel {

	boolean processDropoff(double time, CarsharingRelocationTask task);
	boolean processPickup(double time, CarsharingRelocationTask task);
	void bindTo(CarsharingOperatorMobsim user);
}
