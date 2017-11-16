package org.matsim.contrib.gcs.carsharing;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomers;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperators;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationPowerController;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStations;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicle;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleBattery;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicles;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingCustomerFactory;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingOperatorFactory;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingStationFactory;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingVehicleFactory;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.events.CarsharingDropoffVehicleEvent;
import org.matsim.contrib.gcs.operation.model.CarsharingBatteryModel;
import org.matsim.contrib.gcs.operation.model.CarsharingEnergyConsumptionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingMembershipModel;
import org.matsim.contrib.gcs.operation.model.CarsharingOfferModel;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerDistributionModel;
import org.matsim.contrib.gcs.operation.model.CarsharingPowerSourceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CarsharingManager {
	// injecting factories
	@Inject private Provider<CarsharingParkingModel> parking_model_factory;
	@Inject private Provider<CarsharingPowerDistributionModel> power_distribution_factory;
	@Inject private Provider<CarsharingPowerSourceModel> power_factory;
	@Inject private Provider<CarsharingBatteryModel> battery_factory;
	@Inject private Provider<CarsharingEnergyConsumptionModel> energy_factory;
	@Inject private Provider<CarsharingOfferModel> booking_factory;
	@Inject private Provider<CarsharingUserChoiceModel> user_choice_factory;
	@Inject private Provider<CarsharingMembershipModel> membership_factory;
	@Inject private Provider<CarsharingOperatorChoiceModel> op_choice_factory;
	@Inject private Provider<CarsharingRelocationModel> relocation_factory;
	// others
	@Inject private Set<CarsharingDataProvider> dataProviders;
	@Inject private CarsharingPreprocessedData data;

	
	
	// Main
	private final CarsharingStations stations;
	private final CarsharingVehicles vehicles;
	private final CarsharingCustomers customers;
	private final CarsharingOperators operators;
	private final CarsharingScenario carsharing;
	private final CarsharingDataCollector dataCollector;
	//private final Controler controller;
	private MatsimServices services;
	private MobsimDataProvider mobsimData;
	
	// secondary
	private CarsharingBookingManager booking;
	private CarsharingRelocationModel relocation;
	private int stop_deployment_at_iteration;
	
	
	public CarsharingManager(CarsharingScenario carsharing, Controler controller) {
		this.carsharing = carsharing;
		//this.controller = controller;
		this.stations = CarsharingStationFactory.stations(this.carsharing.getCarNetwork());
		this.vehicles = CarsharingVehicleFactory.vehicles();
		this.customers = CarsharingCustomerFactory.customers();
		this.operators = CarsharingOperatorFactory.operators(this.carsharing.getCarNetwork());
		this.dataCollector = new CarsharingDataCollector();
		stop_deployment_at_iteration = 0;
	}
	
	public CarsharingRelocationModel relocation() { return this.relocation; }
	public Network getCarNetwork() { return this.carsharing.getCarNetwork(); }
	public Scenario getScenario() { return this.carsharing.getScenario(); }
	public CarsharingVehicles vehicles() { return this.vehicles; }
	public CarsharingStations getStations() { return this.stations; }
	public CarsharingCustomers customers() { return this.customers; }
	public CarsharingOperators getOperators() {	return this.operators; }
	public CarsharingBookingManager booking() { return this.booking; }
	public CarsharingDataCollector dataCollector() { return this.dataCollector; }
	public CarsharingConfigGroup getConfig() { return this.carsharing.getConfig(); }
	public CarsharingPreprocessedData ppData() { return this.data; }
	public MobsimDataProvider mobsimData() { return this.mobsimData; }
	public Provider<CarsharingOperatorChoiceModel> opChoiceFactory() { return this.op_choice_factory; }

	public void reset(int iteration) {
		if(this.booking != null) this.booking.reset(iteration);
		if(this.dataCollector != null) this.dataCollector.reset(iteration);
		if(this.relocation != null) this.relocation.reset(iteration);
		for(CarsharingOperatorMobsim operator: this.operators) { operator.reset(iteration);	}
		for(CarsharingCustomerMobsim customer: this.customers) { customer.reset(iteration);	}
		for(CarsharingVehicleMobsim vehicle: this.vehicles) { vehicle.reset(iteration);	}
		for(CarsharingStationMobsim station: this.stations) { 
			station.reset(iteration);
			if(station.parking().getFleetSize() > 0)
				this.services.getEvents().processEvent(
					new CarsharingDropoffVehicleEvent(1.0, this.getScenario(), this, null, station, station.parking().getAll(),	null));
		}		
	}
	
	public void finalizeAndwriteCurrentIterationLogs(int iteration) {
		for(CarsharingVehicleMobsim vtemp: this.vehicles) {
			if(vtemp.status().getTrip() != null) {
				vtemp.endTrip(null, 30 * 3600, "SIMEND");
			}
			if(vtemp.status().getPark() != null) {
				vtemp.endPark(30 * 3600);
			}
		}
		this.dataCollector.writeLogs(iteration);
		new CarsharingScenarioWriter(this).writeXml(this.getConfig().getLogDir() + "/carsharing_scenario_it"+iteration+".xml");
	}
	
	public void setUp(MatsimServices services, MobsimDataProvider mobsimDataProvider) {
		this.services = services;
		this.mobsimData = mobsimDataProvider;
		// relocation
		this.relocation = relocation_factory.get();
		// data collector
		this.dataCollector.addAllModule(dataProviders);
		// booking
		this.booking = new CarsharingBookingManager(this, booking_factory.get());
		// stations
		this.stations.clear();
		for(CarsharingStation station: this.carsharing.getStations().values()) {
			CarsharingStationMobsim stationMobsim = CarsharingStationFactory.
					stationMobsimBuilder(station).
					setParkingModel(parking_model_factory.get()).
					setPowerController(new CarsharingStationPowerController(power_factory.get(), power_distribution_factory.get())).
					build(this.carsharing.getScenario());
			this.stations.add(stationMobsim);
			if(this.booking != null) this.booking.track(stationMobsim);
		}
		// vehicles
		this.vehicles.clear();
		for(CarsharingStationMobsim stationMobsim: this.getStations()) {
			if(this.services.getIterationNumber() > 0 && this.services.getIterationNumber() <= stop_deployment_at_iteration ) {
				Id<ActivityFacility> id = stationMobsim.facility().getId();
				Collection<CarsharingVehicleMobsim> vehs = data.stationMap().get(id).getStation().parking().getAll();
				stationMobsim.initialFleet().clear();
				for(CarsharingVehicleMobsim v : vehs) {
					stationMobsim.initialFleet().put(v.vehicle().getId(), v);
				}
			} 			
			for(Object v: stationMobsim.initialFleet().values()) {
				CarsharingVehicleMobsim vehicleMobsim = CarsharingVehicleFactory.
						vehicleMobsimBuilder((CarsharingVehicle)v).
						setBattery(new CarsharingVehicleBattery(battery_factory.get(), energy_factory.get())).
						build(this.carsharing.getScenario());
				this.vehicles.add(vehicleMobsim);
			}
		}
		this.customers.clear();
		for(Person p : this.carsharing.getScenario().getPopulation().getPersons().values()) {
			membership_factory.get().checkin(p, user_choice_factory.get());
		}
	}	
	
	public void stop_deployment_at_iteration(int depiter) {
		this.stop_deployment_at_iteration = depiter;
	}
	
	
	

}
