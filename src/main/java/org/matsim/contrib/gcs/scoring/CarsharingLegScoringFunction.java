package org.matsim.contrib.gcs.scoring;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.gcs.config.CarsharingConfigGroup;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * 
 * @author haitam
 *
 */
public class CarsharingLegScoringFunction extends CharyparNagelLegScoring {
	
	/**
	 * 
	 * @author haitam
	 *
	 */
	private class RentalData {
		private double startTime;
		private double endTime;
		private double distance;
		private RentalData(double startTime, double endTime, double distance) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.distance = distance;
		}
	}
	
	private final Config config;
	private final ArrayList<RentalData> cssRentals;
	private final CarsharingConfigGroup csConf;
	
	
	public CarsharingLegScoringFunction(ScoringParameters params, Config config,  Network network) {
		super(params, network);
		this.config = config;
		this.csConf = (CarsharingConfigGroup) this.config.getModule(CarsharingConfigGroup.GROUP_NAME);
		this.cssRentals = new ArrayList<RentalData>();	
	}
	
		
	@Override
	public void finish() {		
		super.finish();
		
		if (!cssRentals.isEmpty()) {
			
			double distance = 0.0;
			double time = 0.0;
			
			for(RentalData rental: cssRentals) {
				distance += rental.distance;
				time += (rental.endTime - rental.startTime);
			}
			
			double fixedConstant = this.csConf.getConstantRate();
			double monetaryTimeCostRate_s = this.csConf.getRentalRatePerMin() * 60.;
			double monetaryDistanceCostRate_m = this.params.modeParams.get(CarsharingRouterUtils.cs_drive).monetaryDistanceCostRate;
			
			this.score += fixedConstant + distance * monetaryDistanceCostRate_m + time * monetaryTimeCostRate_s;
		}
		
	}	
	
	
	
	@Override
	public double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		//double tmpScore = super.calcLegScore(departureTime, arrivalTime, leg);
		double tmpScore = 0.0D;
		double travelTime = arrivalTime - departureTime;
		double distance = leg.getRoute().getDistance();
		
		double marginalUtilityOftraveling_s = this.params.modeParams.get(leg.getMode()).marginalUtilityOfTraveling_s;
		double marginalUtilityOfDistance_m = this.params.modeParams.get(leg.getMode()).marginalUtilityOfDistance_m;
		double constantCS = this.params.modeParams.get(leg.getMode()).constant;
		tmpScore +=	constantCS + travelTime * marginalUtilityOftraveling_s + distance * marginalUtilityOfDistance_m;

		return tmpScore;
	}

	
}
