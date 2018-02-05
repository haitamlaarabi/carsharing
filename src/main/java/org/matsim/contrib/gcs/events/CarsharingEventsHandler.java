package org.matsim.contrib.gcs.events;

import org.matsim.core.events.handler.EventHandler;

public interface CarsharingEventsHandler extends EventHandler {
	
	public void handleEvent (CarsharingPickupVehicleEvent event);
	public void handleEvent (CarsharingDropoffVehicleEvent event);
	public void handleEvent (CarsharingChargingStartEvent event);
	public void handleEvent (CarsharingChargingEndEvent event);
	public void handleEvent (CarsharingBookingEvent event);
	public void handleEvent (CarsharingOperatorEvent event);
}
