package org.matsim.contrib.gcs.operation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.gcs.carsharing.AbstractRelocationStrategy;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.core.router.TripRouter;


public class CarsharingRelocationModelImpl extends AbstractRelocationStrategy {
	
	CarsharingManager m;
	TripRouter router;
	
	public CarsharingRelocationModelImpl(
			CarsharingManager m, 
			TripRouter router, 
			Map<String, String> relocation_parameters) {
		super(m, router);
		this.m = m;
		this.router = router;
	}

	@Override
	protected ArrayList<CarsharingOffer> usrelocate( CarsharingDemand demand, List<CarsharingOffer> offers) {
		return new ArrayList<CarsharingOffer>();
	}

	@Override
	protected ArrayList<CarsharingRelocationTask> oprelocate() {
		return new ArrayList<CarsharingRelocationTask>();
	}

	@Override
	protected void update() {
	}

	
}
