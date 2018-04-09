package org.matsim.contrib.gcs.config;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.config.ReflectiveConfigGroup.StringSetter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

/**
 * 
 * @author haitam
 *
 */

public class CarsharingConfigGroup extends ConfigGroup {
	
	public static final String GROUP_NAME = "GENERIC_CAR_SHARING";
	private Map<String, Object> attributes;
	private Config config;
	
	public static final String rentalRatePerMin_str = "rentalRatePerMin";
	public static final String constantRate_str = "constantRate";
	public static final String constant_str = "constant";
	public static final String maxTrain_str = "maxTrain";
	
	public static final String interactionOffset_str = "interactionOffset";
	public static final String searchDistance_str = "searchDistance";
	public static final String activateModule_str = "activateModule";
	public static final String scenarioInputFile_str = "scenarioInputFile";
	
	public static final String logFrequency_str = "logFrequency";
	public static final String tripsLogFile_str = "tripsLogFile";
	public static final String chargingLogFile_str = "chargingLogFile";
	public static final String relocationLogFile_str = "relocationLogFile";
	public static final String bookingLogFile_str = "bookingLogFile";
	
	
	public static final String logDir_str = "logDir";
	
	final CarsharingRelocationParams relocation;
		
	public CarsharingConfigGroup(Config config) {
		super(GROUP_NAME);
		this.config = config;
		attributes = new HashMap<String, Object>();
		this.relocation = new CarsharingRelocationParams();
	}
	
	public CarsharingRelocationParams getRelocation() {
		return relocation;
	}
	
	public PlanCalcScoreConfigGroup.ModeParams getDriveCalcScore() {
		return config.planCalcScore().getOrCreateModeParams(CarsharingRouterUtils.cs_drive);
	}
	
	public PlanCalcScoreConfigGroup.ModeParams getAccessWalkCalcScore() {
		return config.planCalcScore().getOrCreateModeParams(CarsharingRouterUtils.cs_access_walk);
	} 
	
	public PlanCalcScoreConfigGroup.ModeParams getEgressWalkCalcScore() {
		return config.planCalcScore().getOrCreateModeParams(CarsharingRouterUtils.cs_egress_walk);
	}
	
	public PlansCalcRouteConfigGroup.ModeRoutingParams getAccessWalkCalcRoute() {
		return config.plansCalcRoute().getOrCreateModeRoutingParams(CarsharingRouterUtils.cs_access_walk);
	}
	
	public PlansCalcRouteConfigGroup.ModeRoutingParams getEgressWalkCalcRoute() {
		return config.plansCalcRoute().getOrCreateModeRoutingParams(CarsharingRouterUtils.cs_egress_walk);
	}
	
	@StringGetter( maxTrain_str )
	public int getMaxTrain() {
		return (int)attributes.get(maxTrain_str);
	}

	@StringSetter( maxTrain_str )
	public void setMaxTrain(int value) {
		attributes.put(maxTrain_str, value);
	}
	
	
	@StringGetter( logFrequency_str )
	public int getLogFrequency() {
		return (int)attributes.get(logFrequency_str);
	}

	@StringSetter( logFrequency_str )
	public void setLogFrequency(int value) {
		attributes.put(logFrequency_str, value);
	}
	
	@StringGetter( logDir_str )
	public String getLogDir() {
		return (String)attributes.get(logDir_str);
	}

	@StringSetter( logDir_str )
	public void setLogDir(String value) {
		attributes.put(logDir_str, value);
	}
	
	@StringGetter( relocationLogFile_str )
	public String getRelocationLogFile() {
		return (String)attributes.get(relocationLogFile_str);
	}

	@StringSetter( relocationLogFile_str )
	public void setRelocationLogFile(String value) {
		attributes.put(relocationLogFile_str, value);
	}


	@StringGetter( scenarioInputFile_str )
	public String getCarsharingScenarioInputFile() {
		return (String)attributes.get(scenarioInputFile_str);
	}

	@StringSetter( scenarioInputFile_str )
	public void setCarsharingScenarioInputFile(String value) {
		attributes.put(scenarioInputFile_str, value);
	}
	
	@StringGetter( interactionOffset_str )
	public Integer getInteractionOffset() {
		return (Integer)attributes.get(interactionOffset_str);
	}

	@StringSetter( interactionOffset_str )
	public void setInteractionOffset(Integer value) {
		attributes.put(interactionOffset_str, value);
	}

	@StringGetter( searchDistance_str )
	public Double getSearchDistance() {
		return (Double)attributes.get(searchDistance_str);
	}

	@StringSetter( searchDistance_str )
	public void setSearchDistance(Double value) {
		attributes.put(searchDistance_str, value);
	}

	@StringGetter( constantRate_str )
	public Double getConstantRate() {
		return (Double)attributes.get(constantRate_str);
	}

	@StringSetter( constantRate_str )
	public void setConstantRate(Double value) {
		attributes.put(constantRate_str, value);
	}
	
	@StringGetter( rentalRatePerMin_str )
	public Double getRentalRatePerMin() {
		return (Double)attributes.get(rentalRatePerMin_str);
	}

	@StringSetter( rentalRatePerMin_str )
	public void setRentalRatePerMin(Double value) {
		attributes.put(rentalRatePerMin_str, value);
	}

	@StringGetter( activateModule_str )
	public boolean isActivated() {
		return (Boolean)attributes.get(activateModule_str);
	}

	@StringSetter( activateModule_str )
	public void setActivateModule(Boolean value) {
		attributes.put(activateModule_str, value);
	}

	@StringGetter( tripsLogFile_str )
	public String getTripsLogFile() {
		return (String)attributes.get(tripsLogFile_str);
	}

	@StringSetter( tripsLogFile_str )
	public void setTripsLogFile(String value) {
		attributes.put(tripsLogFile_str, value);
	}

	@StringGetter( chargingLogFile_str )
	public String getChargingLogFile() {
		return (String)attributes.get(chargingLogFile_str);
	}

	@StringSetter( chargingLogFile_str )
	public void setChargingLogFile(String value) {
		attributes.put(chargingLogFile_str, value);
	}

	@StringGetter( bookingLogFile_str )
	public String getBookingLogFile() {
		return (String)attributes.get(bookingLogFile_str);
	}

	@StringSetter( bookingLogFile_str )
	public void setBookingLogFile(String value) {
		attributes.put(bookingLogFile_str, value);
	}
	

}
