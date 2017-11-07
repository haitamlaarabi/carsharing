package org.matsim.haitam.api.scoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import com.google.inject.Inject;

public class CarsharingScoringFunctionFactory implements ScoringFunctionFactory {
	
	final Scenario scenario;
	
	final ScoringParametersForPerson params;
	final Map<String, BasicScoring> scoringFuncMap;
	
	@Inject
	public CarsharingScoringFunctionFactory(Scenario scenario) {
		this.scenario = scenario;
		this.params = new SubpopulationScoringParameters( scenario );
		this.scoringFuncMap = new HashMap<String, BasicScoring>();
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public BasicScoring getScoringFunction(String name) {
		return this.scoringFuncMap.get(name);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		this.scoringFuncMap.put(CarsharingLegScoringFunction.class.getSimpleName(), new CarsharingLegScoringFunction(
				params.getScoringParameters( person ),
				this.scenario.getConfig(),
				this.scenario.getNetwork()));
		
		this.scoringFuncMap.put(CharyparNagelLegScoring.class.getSimpleName(), new CharyparNagelLegScoring(
						params.getScoringParameters( person ),
						this.scenario.getNetwork()));
		
		this.scoringFuncMap.put(CarsharingMoneyScoringFunction.class.getSimpleName(), new CarsharingMoneyScoringFunction(
						params.getScoringParameters( person )));

		//the remaining scoring functions can be changed and adapted to the needs of the user
		this.scoringFuncMap.put(CharyparNagelActivityScoring.class.getSimpleName(), new CharyparNagelActivityScoring(
						params.getScoringParameters(
								person ) ) );
		
		this.scoringFuncMap.put(CharyparNagelAgentStuckScoring.class.getSimpleName(), new CharyparNagelAgentStuckScoring(
						params.getScoringParameters(
								person ) ) );
		
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
	    //this is the main difference, since we need a special scoring for carsharing legs
		// ADD HERE
		for(BasicScoring func : this.scoringFuncMap.values()) {
			scoringFunctionSum.addScoringFunction(func);
		}

	    return scoringFunctionSum;
	  }
}
