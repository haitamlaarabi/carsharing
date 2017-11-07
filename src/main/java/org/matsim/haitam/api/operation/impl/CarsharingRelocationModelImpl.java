package org.matsim.haitam.api.operation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.core.router.TripRouter;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.core.CarsharingDemand;
import org.matsim.haitam.api.carsharing.core.CarsharingOffer;
import org.matsim.haitam.api.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingRelocationTask;


public class CarsharingRelocationModelImpl extends AbstractRelocationStrategy {
	
	CarsharingManager m;
	TripRouter router;
	
	public CarsharingRelocationModelImpl(
			CarsharingManager m, 
			TripRouter router, 
			Map<String, String> relocation_parameters) {
		super(m, router, relocation_parameters);
		this.m = m;
		this.router = router;
	}

	@Override
	protected ArrayList<CarsharingOffer> usrelocate( CarsharingDemand demand, List<CarsharingOffer> offers) {
		return new ArrayList<CarsharingOffer>();
	}

	@Override
	protected ArrayList<CarsharingRelocationTask> oprelocate(List<CarsharingOperatorMobsim> operators) {
		return new ArrayList<CarsharingRelocationTask>();
	}

	@Override
	protected void update() {
	}

	@Override
	protected void init() {
	}
	

	
}
