package org.matsim.contrib.gcs.operation.model;

import org.matsim.contrib.gcs.operation.CarsharingOperationModel;

public interface CarsharingEnergyConsumptionModel extends CarsharingOperationModel {
	
	//public abstract double getEnergyConsumptionForLinkInJoule(Link link, double averageSpeedDriven);
	//public abstract double getEnergyConsumptionForLinkInJoule(double drivenDistanceInMeters, double maxSpeedOnLink, double averageSpeedDriven);
	public double calculateEnergyConsumptionInJoule(double speedInMetersPerSecond, double distanceInMeters);

}
