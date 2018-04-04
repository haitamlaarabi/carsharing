package org.matsim.contrib.gcs.operation.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.CarsharingPreprocessedData;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.events.CarsharingBookingEvent;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

public abstract class AbstractRelocationStrategy implements CarsharingRelocationModel {

	private static Logger logger = Logger.getLogger(AbstractRelocationStrategy.class);
	protected final CarsharingManager m;
	protected final TripRouter router;
	protected final CarsharingPreprocessedData pp_data;
	protected final Provider<CarsharingOperatorChoiceModel> choiceFactory;
	protected final Map<String, String> relocation_parameters;
	public final static String ITER_ACTIVATION_PARAM = "ITER_ACTIVATION";
	public final static String TIME_BIN_PARAM = "TIME_BIN";
	public final static String LB_TIME_BIN_PARAM = "LB_TIME_BIN";
	public final static String UB_TIME_BIN_PARAM = "UB_TIME_BIN";
	public final static String STEP_TIME_BIN_PARAM = "STEP_TIME_BIN";
	public final static String PERF_FILE_PARAM = "PERF_FILE";
	protected final int iter_activation;
	protected int time_bin;
	protected final int lb_time_bin;
	protected final int ub_time_bin;
	protected final int step_time_bin;
	
	
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
		if(relocation_parameters.containsKey(TIME_BIN_PARAM)) {
			this.lb_time_bin = Integer.parseInt(relocation_parameters.get(TIME_BIN_PARAM));
			this.ub_time_bin = Integer.parseInt(relocation_parameters.get(TIME_BIN_PARAM));
			this.step_time_bin = 0;
		} else if(relocation_parameters.containsKey(LB_TIME_BIN_PARAM)) {
			this.lb_time_bin = Integer.parseInt(relocation_parameters.get(LB_TIME_BIN_PARAM));
			this.ub_time_bin = Integer.parseInt(relocation_parameters.get(UB_TIME_BIN_PARAM));
			this.step_time_bin = Integer.parseInt(relocation_parameters.get(STEP_TIME_BIN_PARAM));
		} else {
			this.lb_time_bin = 1800;
			this.ub_time_bin = 1800;
			this.step_time_bin = 0;
		}
		this.time_bin = this.lb_time_bin;
	}
	
	@Override
	public void updateRelocationList(int time) {
		if(this.time_step.check((int) time)) {
			this.update();
		}
	}
	
	@Override
	public List<CarsharingOffer> relocationList(int time, CarsharingDemand demand, List<CarsharingOffer> offers) {
		if(this.iter >= this.iter_activation) {
			return this.usrelocate(demand, offers);
		}
		return new ArrayList<CarsharingOffer>();
	}
	
	@Override
	public List<CarsharingRelocationTask> relocationList(int time) {
		List<CarsharingRelocationTask> booked_tasks = new ArrayList<CarsharingRelocationTask>();
		if(this.iter >= this.iter_activation) {
			List<CarsharingRelocationTask> tasks = this.oprelocate();
			CarsharingRelocationTask sTask = null;
			double accessTime = 0, accessDistance = 0;
			List<CarsharingRelocationTask> temp_tasks = new ArrayList<CarsharingRelocationTask>();
			for(CarsharingRelocationTask t : tasks) {
				CarsharingOperatorMobsim op = (CarsharingOperatorMobsim) t.getAgent();
				if(!op.available()) {
					throw new RuntimeException("Adding tasks to busy operator");
				}
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
							for(CarsharingRelocationTask new_task : temp_tasks) {
								new_task.setBooking(b);
								booked_tasks.add(new_task);
								op.addTask(new_task);
							}
						} else {
							logger.warn("BOOKING FAILURE - " + b.getAgent().getId());
							if(!b.vehicleOffer()) {
								b.setComment("NO_VEHICLE_TO_RELOCATE");
							} else if(!b.parkingOffer()) {
								b.setComment("NO_PARKING_SPACE");
							}
						}
						m.events().processEvent(new CarsharingBookingEvent(time, m.getScenario(), m, b.getDemand(), b));
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
		builder.setAccess(sTask.getTime(), sTask.getStation(), aTime, aDist);
		builder.setEgress(eTask.getTime(), eTask.getStation(), 0, 0);
		builder.setDrive(eTask.getSize(), eTask.getRoute());
		builder.setCost(0);
		CarsharingBookingRecord booking = CarsharingBookingRecord.constructAndGetBookingRec(time_step.step(), builder.build());
		return booking;
	}
	
	
	@Override
	public void reset(int iteration) {
		PrintWriter perfWriter = null;
		try {
			perfWriter = new PrintWriter(new BufferedWriter(new FileWriter(relocation_parameters.get(PERF_FILE_PARAM), true)));
			perfWriter.println("iteration\tbin\tstation.id\tvalue\tvariable");
			for(CarsharingStationMobsim s : this.m.getStations()) {
				CarsharingBookingStation b = this.m.booking().track(s);
				int dropoff_failed = 0;
				int pickup_failed = 0;
				int dropoff_success = 0;
				int pickup_success = 0;
				for(CarsharingBookingRecord r : b.getDemand(Integer.MAX_VALUE)) {
					if(r.bookingFailed()) pickup_failed++;
					else pickup_success++;
				}
				for(CarsharingBookingRecord r : b.getSupply(Integer.MAX_VALUE)) {
					if(r.bookingFailed()) dropoff_failed++;
					else dropoff_success++;
				}
				double performance = pickup_success/(pickup_success+pickup_failed) + dropoff_success/(dropoff_success+dropoff_failed);
				perfWriter.println(
						iteration + "\t" + 
						this.time_bin + "\t" + 
						s.getId().toString() + "\t" +
						dropoff_failed + "\tDPf\t" + 
						pickup_failed + "\tPUf\t" + 
						dropoff_success + "\tDPs\t" + 
						pickup_success + "\tPUs\t" + 
						performance + "\tp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.time_bin += this.step_time_bin;
		if(this.time_bin > this.ub_time_bin) {
			logger.warn("bin time went beyond upper bound");
		}
		this.time_step = new TimeStep(0, this.time_bin);
		this.iter = iteration;
	}
	
	// ***********************************************
	// ***********************************************
	
	protected TimeStep time_step;
	protected int iter = -1;
	
	protected abstract List<CarsharingOffer> usrelocate(CarsharingDemand demand, List<CarsharingOffer> offers);
	protected abstract List<CarsharingRelocationTask> oprelocate();
	protected abstract void update();
	
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
