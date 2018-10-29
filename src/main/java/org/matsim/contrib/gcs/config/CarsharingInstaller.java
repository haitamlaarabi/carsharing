package org.matsim.contrib.gcs.config;

import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.CarsharingPreprocessedData;
import org.matsim.contrib.gcs.carsharing.CarsharingScenario;
import org.matsim.contrib.gcs.carsharing.CarsharingScenarioReader;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDataCollector.CarsharingDataProvider;
import org.matsim.contrib.gcs.control.ControllerListener;
import org.matsim.contrib.gcs.operation.impl.CarsharingBatteryModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingEnergyConsumptionModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingMembershipModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingOfferModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingOperatorChoiceModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingParkingModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingPowerDistributionModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingPowerSourceModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingRelocationModelImpl;
import org.matsim.contrib.gcs.operation.impl.CarsharingUserChoiceModelImpl;
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
import org.matsim.contrib.gcs.qsim.CarsharingMobsimHandle;
import org.matsim.contrib.gcs.qsim.CarsharingQsimFactory;
import org.matsim.contrib.gcs.replanning.CarsharingMainModeIdentifier;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.contrib.gcs.router.CarsharingDirectRouterModule;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
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
	
	public CarsharingInstaller(Scenario scenario, CarsharingScenario carsharing, Controler controler, String rootdir) {
		this.carsharing = carsharing;
		this.controler = controler;
		this.scenario = scenario;
		if(this.carsharing == null) {
			this.carsharing = new CarsharingScenario(scenario, rootdir);
		} 
		this.manager = new CarsharingManager(this.carsharing, this.controler);
	}

	public CarsharingInstaller(Scenario scenario, Controler controler, String rootdir) {
		this(scenario, null, controler, rootdir);
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
    	bindListener(ControllerListener.class);
    	
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
        
      	addTravelTimeBinding(CarsharingRouterUtils.cs_drive).to(networkTravelTime());
    	addTravelDisutilityFactoryBinding(CarsharingRouterUtils.cs_drive).to(carTravelDisutilityFactoryKey());
    	addRoutingModuleBinding(CarsharingRouterUtils.cs_drive).to(Key.get(RoutingModule.class, Names.named(TransportMode.car)));
    	
    	
    	//addTravelDisutilityFactoryBinding(CarsharingRouterUtils.cs_pt).to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.pt)));
    	addRoutingModuleBinding(CarsharingRouterUtils.cs_pt).to(Key.get(RoutingModule.class, Names.named(TransportMode.pt)));
    	
    	//addTravelDisutilityFactoryBinding(CarsharingRouterUtils.cs_walk).to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.walk)));
    	addRoutingModuleBinding(CarsharingRouterUtils.cs_walk).to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
    	
    	addControlerListenerBinding().to(defaultListener);
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
	
	Class<? extends ControllerListener> defaultListener = null;
	
	Class<? extends CarsharingMobsimHandle> monitor = null;
	//Class<? extends CarsharingRelocationModel> relocate = null;
	Class<? extends CarsharingOfferModel> offermodel = null;
	
	public final void bindListener(Class<? extends ControllerListener> clazz) {
		defaultListener = clazz;
	}
	
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
