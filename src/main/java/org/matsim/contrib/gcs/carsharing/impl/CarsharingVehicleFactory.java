package org.matsim.contrib.gcs.carsharing.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicle;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleBattery;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicles;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CarsharingVehicleFactory {
	
	public static CarsharingVehicleBuilder vehicleBuilder(Scenario scenario, String id) {
		return new CarsharingVehicleBuilder(scenario, id);
	}
	
	public static CarsharingVehicleMobsimBuilder vehicleMobsimBuilder(CarsharingVehicle vehicle) {
		return new CarsharingVehicleMobsimBuilder(vehicle);
	}
	
	public static CarsharingVehicles vehicles() {
		return new CarsharingVehicles() {
			private final Map<Id, CarsharingVehicleMobsim> vehiclesmap = new HashMap<Id, CarsharingVehicleMobsim>();
			@Override
			public Map<Id, CarsharingVehicleMobsim> map() {
				return Collections.unmodifiableMap(vehiclesmap);
			}
			@Override
			public void add(CarsharingVehicleMobsim vehicle) {
				if(!vehiclesmap.containsKey(vehicle.vehicle().getId())) {
					vehiclesmap.put(vehicle.vehicle().getId(), vehicle);
				} else {
					throw new RuntimeException("vehicle with id " + vehicle.vehicle().getId() + " already exist!");
				}
			}
			@Override
			public int size() {
				return vehiclesmap.size();
			}
			@Override
			public Iterator<CarsharingVehicleMobsim> iterator() {
				return this.vehiclesmap.values().iterator();
			}
			@Override
			public void clear() {
				vehiclesmap.clear();
			}
		};
	}
	
	
	
	public static class CarsharingVehicleBuilder {
		CarsharingVehicleImpl carsharing;
		private CarsharingVehicleBuilder(Scenario scenario, String id) {
			VehicleType vehicleType = VehicleUtils.getDefaultVehicleType();
			//vehicleType.setMaximumVelocity(6.94444);
			if(!scenario.getVehicles().getVehicleTypes().containsKey(vehicleType.getId())){
				scenario.getVehicles().addVehicleType(vehicleType);
			}
			Id<Vehicle> idv = Id.create(id, Vehicle.class);
			Vehicle v = scenario.getVehicles().getVehicles().get(idv);
			if(v == null) {
				v = VehicleUtils.getFactory().createVehicle(idv, vehicleType);
				scenario.getVehicles().addVehicle(v);
			}
			this.carsharing = new CarsharingVehicleImpl(v);
			this.carsharing.setType("Default");
		}
		public CarsharingVehicleBuilder setName(String name){
			carsharing.setName(name);
			return this;
		}
		public CarsharingVehicleBuilder setType(String type) {
			carsharing.setType(type);
			return this;
		}
		public CarsharingVehicle build(){
			return this.carsharing;
		}
	}
	
	
	public static class CarsharingVehicleMobsimBuilder {
		CarsharingVehicleImpl carsharing;
		private CarsharingVehicleMobsimBuilder(CarsharingVehicle vehicle) {
			this.carsharing = (CarsharingVehicleImpl)vehicle;
		}
		public CarsharingVehicleMobsimBuilder setBattery(CarsharingVehicleBattery battery) {
			carsharing.battery = battery;
			carsharing.battery.reset();
			return this;
		}
		public CarsharingVehicleMobsim build(Scenario scenario) {
			return this.carsharing;
		}
		
	}

}
