package org.matsim.contrib.gcs.scoring;

import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

public class CarsharingMoneyScoringFunction implements BasicScoring, MoneyScoring {
	
	private double score;
	private final double marginalUtilityOfMoney;
	
	
	public CarsharingMoneyScoringFunction(final ScoringParameters params) {
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney;
	}

	public CarsharingMoneyScoringFunction(final double marginalUtilityOfMoney) {
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
	}
	
	
	@Override
	public void addMoney(final double amount) {
		this.score += amount * this.marginalUtilityOfMoney ; // linear mapping of money to score
	}
	

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}


}
