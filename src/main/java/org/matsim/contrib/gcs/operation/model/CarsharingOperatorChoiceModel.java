package org.matsim.contrib.gcs.operation.model;

import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingOperatorChoiceModel extends CarsharingOperationModel {

	boolean processDropoff(double time, CarsharingRelocationTask task);
	boolean processPickup(double time, CarsharingRelocationTask task);
	void bindTo(CarsharingOperatorMobsim user);
}
