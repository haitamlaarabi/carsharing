package org.matsim.haitam.api.control;

import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.events.AbstractCarsharingEvent;
import org.matsim.haitam.api.events.CarsharingBookingEvent;
import org.matsim.haitam.api.events.CarsharingChargingEndEvent;
import org.matsim.haitam.api.events.CarsharingChargingStartEvent;
import org.matsim.haitam.api.events.CarsharingDropoffVehicleEvent;
import org.matsim.haitam.api.events.CarsharingEventsHandler;
import org.matsim.haitam.api.events.CarsharingPickupVehicleEvent;


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

}
