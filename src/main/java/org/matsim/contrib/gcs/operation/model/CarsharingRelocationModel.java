package org.matsim.contrib.gcs.operation.model;

import java.util.List;

import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingRelocationModel extends CarsharingOperationModel {
	
	
	List<CarsharingOffer> relocationList(int time, CarsharingDemand demand, List<CarsharingOffer> offers);
	List<CarsharingRelocationTask> relocationList(int time);
	void reset(int iteration);
	void updateRelocationList(int time);
	boolean isActivated();
}
