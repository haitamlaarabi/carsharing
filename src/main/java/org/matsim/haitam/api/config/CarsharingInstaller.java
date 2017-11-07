package org.matsim.haitam.api.config;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.CarsharingPreprocessedData;
import org.matsim.haitam.api.carsharing.CarsharingScenario;
import org.matsim.haitam.api.carsharing.CarsharingScenarioReader;
import org.matsim.haitam.api.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.haitam.api.control.ControllerListener;
import org.matsim.haitam.api.operation.impl.CarsharingBatteryModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingEnergyConsumptionModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingMembershipModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingOfferModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingOperatorChoiceModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingParkingModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingPowerDistributionModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingPowerSourceModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingRelocationModelImpl;
import org.matsim.haitam.api.operation.impl.CarsharingUserChoiceModelImpl;
import org.matsim.haitam.api.operation.model.CarsharingBatteryModel;
import org.matsim.haitam.api.operation.model.CarsharingEnergyConsumptionModel;
import org.matsim.haitam.api.operation.model.CarsharingMembershipModel;
import org.matsim.haitam.api.operation.model.CarsharingOfferModel;
import org.matsim.haitam.api.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.haitam.api.operation.model.CarsharingParkingModel;
import org.matsim.haitam.api.operation.model.CarsharingPowerDistributionModel;
import org.matsim.haitam.api.operation.model.CarsharingPowerSourceModel;
import org.matsim.haitam.api.operation.model.CarsharingRelocationModel;
import org.matsim.haitam.api.operation.model.CarsharingUserChoiceModel;
import org.matsim.haitam.api.qsim.CarsharingMobsimHandle;
import org.matsim.haitam.api.qsim.CarsharingQsimFactory;
import org.matsim.haitam.api.replanning.CarsharingMainModeIdentifier;
import org.matsim.haitam.api.replanning.CarsharingPlanModeCst;
import org.matsim.haitam.api.router.CarsharingDirectRouterModule;
import org.matsim.haitam.api.router.CarsharingRouterModeCst;
import org.matsim.haitam.api.scoring.CarsharingScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorModule;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;


public abstract class CarsharingInstaller extends AbstractModule {
	
	public final CarsharingManager manager;
	public final Controler controler;
	public final Scenario scenario;
	public CarsharingScenario carsharing = null;
	private Multibinder<CarsharingDataProvider> dataProviderMultibinder = null;
	
	public CarsharingScenario getCarsharingScenario() {
		return this.carsharing;
	}
	
	public Scenario getScenario() {
		return this.scenario;
	}
	
	public CarsharingManager getCarsharingManager() {
		return this.manager;
	}
	
	public CarsharingInstaller(Scenario scenario, CarsharingScenario carsharing, Controler controler, String logdir) {
		this.carsharing = carsharing;
		this.controler = controler;
		this.scenario = scenario;
		if(this.carsharing == null) {
			this.carsharing = new CarsharingScenario(scenario, logdir);
		} 
		this.manager = new CarsharingManager(this.carsharing, this.controler);
	}

	public CarsharingInstaller(Scenario scenario, Controler controler, String logdir) {
		this(scenario, null, controler, logdir);
	}
	
	public void init() {
		if(this.carsharing.getStations().isEmpty()) {
			new CarsharingScenarioReader(this.carsharing, scenario).readXml(this.carsharing.getConfig().getCarsharingScenarioInputFile());
		}
	}
		
	@Override
	public void install() {
		dataProviderMultibinder = Multibinder.newSetBinder(binder(), CarsharingDataProvider.class);
		bind(CarsharingManager.class).toInstance(manager);	
		bind(Controler.class).toInstance(controler);
		
		/*bind(TravelTimeCollector.class);
		addEventHandlerBinding().to(TravelTimeCollector.class);
		bindNetworkTravelTime().to(TravelTimeCollector.class);
		addMobsimListenerBinding().to(TravelTimeCollector.class);
		bind(TravelTime.class).to(TravelTimeCollector.class);
        //bind(WithinDayEngine.class);
        bind(FixedOrderSimulationListener.class).asEagerSingleton();
        //bind(WithinDayControlerListener.class).asEagerSingleton();
        addControlerListenerBinding().to(WithinDayControlerListener.class);
        bind(MobsimDataProvider.class).asEagerSingleton();
        bind(ActivityReplanningMap.class).asEagerSingleton();
        bind(LinkReplanningMap.class).asEagerSingleton();
        bind(EarliestLinkExitTimeProvider.class).asEagerSingleton();*/
		
    	bindCarsharingUserChoiceModel(CarsharingUserChoiceModelFactory.class);
    	bindCarsharingBatteryModel(CarsharingBatteryModelFactory.class);
    	bindCarsharingEnergyConsumptionModel(CarsharingEnergyConsumptionModelFactory.class);
    	bindCarsharingPowerDistributionModel(CarsharingPowerDistributionModelFactory.class);
    	bindCarsharingPowerSourceModel(CarsharingPowerSourceModelFactory.class);
    	bindCarsharingParkingModel(CarsharingParkingModelFactory.class);
    	bindCarsharingOfferModel(CarsharingOfferModelImpl.class);
    	bindCarsharingMembershipModel(CarsharingMembershipModelFactory.class);
    	bindCarsharingRelocationModel(CarsharingRelocationModelFactory.class);
    	bindCarsharingOperatorChoiceModel(CarsharingOperatorChoiceModelFactory.class);
    	bindCarsharingMobsimMonitoring(CarsharingMobsimHandleImpl.class);
    	
    	installOrOverrideModules(); // NEW MODULES TO ADD
    	
		bind(CarsharingUserChoiceModel.class).toProvider(this.userchoicemodel);
		bind(CarsharingBatteryModel.class).toProvider(this.batterymodel);
		bind(CarsharingEnergyConsumptionModel.class).toProvider(this.energyconsmodel);
		bind(CarsharingPowerDistributionModel.class).toProvider(this.powerdistrmodel);
		bind(CarsharingPowerSourceModel.class).toProvider(this.powersrcmodel);
		bind(CarsharingParkingModel.class).toProvider(this.parkingmodel);
		bind(CarsharingMembershipModel.class).toProvider(this.membershipmodel);
		bind(CarsharingOperatorChoiceModel.class).toProvider(this.opchoicemodel);
		bind(CarsharingRelocationModel.class).toProvider(this.relocate);
		bind(CarsharingOfferModel.class).to(this.offermodel);
		bind(CarsharingPreprocessedData.class).toInstance(new CarsharingPreprocessedData());
		bind(CarsharingMobsimHandle.class).to(this.monitor).asEagerSingleton();

		bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
		bindMobsim().toProvider( CarsharingQsimFactory.class );
		
        addRoutingModuleBinding(CarsharingPlanModeCst.directTrip).to(CarsharingDirectRouterModule.class);
        bind(MainModeIdentifier.class).to(CarsharingMainModeIdentifier.class);
        
      	addTravelTimeBinding(CarsharingRouterModeCst.cs_drive).to(networkTravelTime());
    	addTravelDisutilityFactoryBinding(CarsharingRouterModeCst.cs_drive).to(carTravelDisutilityFactoryKey());
    	addRoutingModuleBinding(CarsharingRouterModeCst.cs_drive).to(Key.get(RoutingModule.class, Names.named(TransportMode.car)));
    	
    	addControlerListenerBinding().to(ControllerListener.class);
    	
	}
	


	public abstract void installOrOverrideModules();
	
	
	Class<? extends Provider<CarsharingOperatorChoiceModel>> opchoicemodel = null;
	Class<? extends Provider<CarsharingUserChoiceModel>> userchoicemodel = null;
	Class<? extends Provider<CarsharingBatteryModel>> batterymodel = null;
	Class<? extends Provider<CarsharingEnergyConsumptionModel>> energyconsmodel = null;
	Class<? extends Provider<CarsharingPowerDistributionModel>> powerdistrmodel = null;
	Class<? extends Provider<CarsharingPowerSourceModel>> powersrcmodel = null;
	Class<? extends Provider<CarsharingParkingModel>> parkingmodel = null;
	Class<? extends Provider<CarsharingMembershipModel>> membershipmodel = null;
	Class<? extends Provider<CarsharingRelocationModel>> relocate = null;
	
	Class<? extends CarsharingMobsimHandle> monitor = null;
	//Class<? extends CarsharingRelocationModel> relocate = null;
	Class<? extends CarsharingOfferModel> offermodel = null;
	
	
	public final void bindCarsharingRelocationModel(Class<? extends Provider<CarsharingRelocationModel>> factory) {
		this.relocate = factory;
	}
	public final void bindCarsharingMobsimMonitoring(Class<? extends CarsharingMobsimHandle> clazz) {
		this.monitor = clazz;
	}
	public final void bindCarsharingUserChoiceModel(Class<? extends Provider<CarsharingUserChoiceModel>> factory) {
		this.userchoicemodel = factory;
	}
	public final void bindCarsharingBatteryModel(Class<? extends Provider<CarsharingBatteryModel>> factory) {
		this.batterymodel = factory;
	}
	public final void bindCarsharingEnergyConsumptionModel(Class<? extends Provider<CarsharingEnergyConsumptionModel>> factory) {
		this.energyconsmodel = factory;
	}
	public final void bindCarsharingPowerDistributionModel(Class<? extends Provider<CarsharingPowerDistributionModel>> factory) {
		this.powerdistrmodel = factory;
	}
	public final void bindCarsharingPowerSourceModel(Class<? extends Provider<CarsharingPowerSourceModel>> factory) {
		this.powersrcmodel = factory;
	}
	public final void bindCarsharingParkingModel(Class<? extends Provider<CarsharingParkingModel>> factory) {
		this.parkingmodel = factory;
	}
	public final void bindCarsharingOfferModel(Class<? extends CarsharingOfferModel> clazz) {
		this.offermodel = clazz;
	}
	public final void bindCarsharingMembershipModel(Class<? extends Provider<CarsharingMembershipModel>> factory) {
		this.membershipmodel = factory;
	}
	public final void addCarsharingDataProvider(Class<? extends CarsharingDataProvider> clazz) {
		dataProviderMultibinder.addBinding().to(clazz);
	}
	public final void bindCarsharingOperatorChoiceModel(Class<? extends Provider<CarsharingOperatorChoiceModel>> factory) {
		this.opchoicemodel = factory;
	}
	
	public static class CarsharingMobsimHandleImpl extends CarsharingMobsimHandle {
		@Override
		protected void execute(double time) {
		}
	}
	public static class CarsharingRelocationModelFactory implements Provider<CarsharingRelocationModel> {	
		@Inject CarsharingManager m;
		@Inject TripRouter router;
		@Override
		public CarsharingRelocationModel get() {
			return new CarsharingRelocationModelImpl(m, router, new HashMap<String, String>());
		}
	}
	public static class CarsharingMembershipModelFactory implements Provider<CarsharingMembershipModel> {
		@Inject CarsharingManager m;
		CarsharingMembershipModel singleton = null;
		@Override
		public CarsharingMembershipModel get() {
			if(singleton == null) {
				singleton = new CarsharingMembershipModelImpl(m, false);
			}
			return singleton;
		}
	}
	public static class CarsharingBatteryModelFactory implements Provider<CarsharingBatteryModel> {
		@Override
		public CarsharingBatteryModel get() {
			return new CarsharingBatteryModelImpl();
		}
	}
	public static class CarsharingEnergyConsumptionModelFactory implements Provider<CarsharingEnergyConsumptionModel> {
		@Override public CarsharingEnergyConsumptionModel get() {
			return new CarsharingEnergyConsumptionModelImpl();
		}
	}
	public static class CarsharingOperatorChoiceModelFactory implements Provider<CarsharingOperatorChoiceModel> {
		@Inject CarsharingManager m;
		@Override public CarsharingOperatorChoiceModel get() {
			return new CarsharingOperatorChoiceModelImpl(m);
		}
	}
	public static class CarsharingPowerDistributionModelFactory implements Provider<CarsharingPowerDistributionModel> {
		@Inject CarsharingManager m;
		@Inject EventsManager em;
		@Override public CarsharingPowerDistributionModel get() {
			return new CarsharingPowerDistributionModelImpl(m, em);
		}
	}
	public static class CarsharingPowerSourceModelFactory implements Provider<CarsharingPowerSourceModel> {
		@Override
		public CarsharingPowerSourceModel get() {
			// 16.6 * 1000; // J/s (or 16,6 kW)
			//3.3 * 1000 ; // J/s (or 3,3 kW)
			return new CarsharingPowerSourceModelImpl(16.6 * 1000);
		}
	}
	public static class CarsharingUserChoiceModelFactory implements Provider<CarsharingUserChoiceModel> {
		@Override public CarsharingUserChoiceModel get() {
			return new CarsharingUserChoiceModelImpl();
		}
	}
	public static class CarsharingParkingModelFactory implements Provider<CarsharingParkingModel> {
		@Override public CarsharingParkingModel get() {
			return new CarsharingParkingModelImpl();
		}
	}

}
