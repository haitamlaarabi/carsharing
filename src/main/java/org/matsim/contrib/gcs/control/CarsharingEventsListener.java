package org.matsim.contrib.gcs.control;

import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.events.AbstractCarsharingEvent;
import org.matsim.contrib.gcs.events.CarsharingBookingEvent;
import org.matsim.contrib.gcs.events.CarsharingChargingEndEvent;
import org.matsim.contrib.gcs.events.CarsharingChargingStartEvent;
import org.matsim.contrib.gcs.events.CarsharingDropoffVehicleEvent;
import org.matsim.contrib.gcs.events.CarsharingEventsHandler;
import org.matsim.contrib.gcs.events.CarsharingOperatorEvent;
import org.matsim.contrib.gcs.events.CarsharingPickupVehicleEvent;


public class CarsharingEventsListener implements CarsharingEventsHandler {
	
	private CarsharingManager manager;
	
	public CarsharingEventsListener(CarsharingManager manager) {
		this.manager = manager;
	}
	
	private void LOG(AbstractCarsharingEvent event) {
		this.manager.dataCollector().addLog(event, event.getTime());
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(CarsharingPickupVehicleEvent event) {
		LOG(event);
	}

	@Override
	public void handleEvent(CarsharingDropoffVehicleEvent event) {
		LOG(event);
	}

	@Override
	public void handleEvent(CarsharingChargingEndEvent event) {
		LOG(event);
		
	}

	@Override
	public void handleEvent(CarsharingChargingStartEvent event) {
		LOG(event);
	}

	@Override
	public void handleEvent(CarsharingBookingEvent event) {
		LOG(event);
	}

	@Override
	public void handleEvent(CarsharingOperatorEvent event) {
		LOG(event);
	}

}
