package org.matsim.contrib.gcs.operation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.CarsharingPreprocessedData;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

public abstract class AbstractRelocationStrategy implements CarsharingRelocationModel {

	protected final CarsharingManager m;
	protected final TripRouter router;
	protected final CarsharingPreprocessedData pp_data;
	protected final Provider<CarsharingOperatorChoiceModel> choiceFactory;
	protected final Map<String, String> relocation_parameters;
	public final static String ITER_ACTIVATION_PARAM = "ITER_ACTIVATION";
	public final static String TIME_BIN_PARAM = "TIME_BIN";
	protected final int iter_activation;
	protected final int time_bin;
	
	
	public AbstractRelocationStrategy(
			CarsharingManager m, 
			TripRouter router, 
			Map<String, String> relocation_parameters) {
		this.m = m;
		this.router = router;
		this.choiceFactory = m.opChoiceFactory();
		this.pp_data = m.ppData();
		this.relocation_parameters = relocation_parameters;
		if(relocation_parameters.containsKey(ITER_ACTIVATION_PARAM)) {
			this.iter_activation = Integer.parseInt(relocation_parameters.get(ITER_ACTIVATION_PARAM));
		} else {
			this.iter_activation = -1;
		}
		if(relocation_parameters.containsKey(ITER_ACTIVATION_PARAM)) {
			this.time_bin = Integer.parseInt(relocation_parameters.get(TIME_BIN_PARAM));
		} else {
			this.time_bin = 3600;
		}
	}
	
	@Override
	public void updateRelocationList(double time) {
		if(this.time_step.check((int) time)) {
			this.update();
		}
	}
	
	@Override
	public List<CarsharingOffer> relocationList(double time, CarsharingDemand demand, List<CarsharingOffer> offers) {
		if(this.iter >= this.iter_activation) {
			return this.usrelocate(demand, offers);
		}
		return new ArrayList<CarsharingOffer>();
	}
	
	@Override
	public List<CarsharingRelocationTask> relocationList(double time) {
		List<CarsharingRelocationTask> booked_tasks = new ArrayList<CarsharingRelocationTask>();
		if(this.iter >= this.iter_activation) {
			List<CarsharingRelocationTask> tasks = this.oprelocate();
			CarsharingRelocationTask sTask = null;
			double accessTime = 0, accessDistance = 0;
			List<CarsharingRelocationTask> temp_tasks = new ArrayList<CarsharingRelocationTask>();
			for(CarsharingRelocationTask t : tasks) {
				temp_tasks.add(t);
				if(t.getSize() == 0) {
					accessTime = t.getTravelTime();
					accessDistance = t.getDistance();
				} else {
					if(t.getType().equals("START")) {
						sTask = t;
					} else {
						CarsharingBookingRecord b = constructBookingRecord(sTask, t, accessTime, accessDistance);
						if(this.m.booking().process(b)) { // **** BOOKING 
							CarsharingOperatorMobsim op = (CarsharingOperatorMobsim) t.getAgent();
							for(CarsharingRelocationTask t2 : temp_tasks) {
								t2.setBooking(b);
								booked_tasks.add(t2);
								op.addTask(t2);
							}
						}
						sTask = null;
						accessTime = 0;
						accessDistance = 0;
						temp_tasks.clear();
					}
				}
			}
		}
		return booked_tasks;
	}
	
	CarsharingBookingRecord constructBookingRecord(CarsharingRelocationTask sTask, CarsharingRelocationTask eTask, double aTime, double aDist) {
		Leg reloLeg = PopulationUtils.createLeg("RELOCATION");
		Activity reloSrcActivity = PopulationUtils.createActivityFromCoordAndLinkId("STATION", sTask.getStation().facility().getCoord(), sTask.getStation().facility().getLinkId());
		reloSrcActivity.setEndTime(sTask.getTime());
		Activity reloDstActivity = PopulationUtils.createActivityFromCoordAndLinkId("STATION", eTask.getStation().facility().getCoord(), eTask.getStation().facility().getLinkId());
		reloDstActivity.setStartTime(eTask.getTime());
		CarsharingDemand d = new CarsharingDemand(reloLeg, eTask.getAgent(), reloSrcActivity, reloDstActivity, eTask.getSize());
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromDemand(d, "OPERATOR_RELOCATION");
		builder.setAccess(sTask.getTime(), sTask.getStation(), aTime, aDist, m.getConfig().getInteractionOffset());
		builder.setEgress(eTask.getTime(), eTask.getStation(), 0, 0, m.getConfig().getInteractionOffset());
		builder.setDrive(eTask.getSize(), eTask.getRoute());
		builder.setCost(0);
		CarsharingBookingRecord booking = CarsharingBookingRecord.constructAndGetBookingRec(time_step.step(), builder.build());
		return booking;
	}
	
	@Override
	public void reset(int iteration) {
		this.time_step = new TimeStep(0, time_bin);
		this.iter = iteration;
	}
	
	// ***********************************************
	// ***********************************************
	
	protected TimeStep time_step;
	protected int iter = -1;
	
	protected abstract List<CarsharingOffer> usrelocate(CarsharingDemand demand, List<CarsharingOffer> offers);
	protected abstract List<CarsharingRelocationTask> oprelocate();
	protected abstract void update();
	//protected abstract void init();
	
	protected class TimeStep {
		protected int time;
		protected int time_step;
		protected int timeBin;
		public TimeStep(int t, int timeBin) {
			this.time = t;
			this.timeBin = timeBin;
			this.time_step = step();
		}
		public TimeStep next() { return new TimeStep(time + timeBin, timeBin); }
		public TimeStep prev() { return new TimeStep(time - timeBin, timeBin); }
		public int step() { return (int)((int)(this.time/timeBin)*timeBin); }
		public int get() { return time; }
		public int getBin() { return timeBin; }
		public void set(int t) { time = t; }
		public boolean check(int t) {
			this.time = t;
			if(step() > this.time_step) {	
				this.time_step = step(); 
				return true;
			}
			return false;
		}
		public int getK() {
			return (int)(this.time/timeBin);
		}
	}

}
