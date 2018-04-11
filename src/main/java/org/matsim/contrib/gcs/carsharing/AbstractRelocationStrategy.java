package org.matsim.contrib.gcs.carsharing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOffer;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingRelocationTask;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationDemand;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingOperatorFactory;
import org.matsim.contrib.gcs.config.CarsharingRelocationParams;
import org.matsim.contrib.gcs.events.CarsharingBookingEvent;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.contrib.gcs.operation.model.CarsharingRelocationModel;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.Provider;

public abstract class AbstractRelocationStrategy implements CarsharingRelocationModel {
	
	static int STEP = 5*60; // 5 min
	
	private static Logger logger = Logger.getLogger(AbstractRelocationStrategy.class);
	protected final CarsharingManager m;
	protected final TripRouter router;
	protected final CarsharingPreprocessedData pp_data;
	protected final Provider<CarsharingOperatorChoiceModel> choiceFactory;
	protected CarsharingRelocationParams rparams;
	protected final ConcurrentHashMap<Id<ActivityFacility>, CarsharingStationDemand> demand;
	
	
	protected PrintWriter perf_writer;
	protected PrintWriter traceWriter;
	protected PrintWriter taskWriter;
	
	protected int time_bin_k_ub = 0;
	protected int time_bin_k = 0;
	protected int time_bin;
	
	protected int staff_size_k_ub = 0;
	protected int staff_size_k = 0;
	protected int staff_size;
	protected int train_size;
	
	public AbstractRelocationStrategy(CarsharingManager m, TripRouter router) {
		this.m = m;
		this.router = router;
		this.choiceFactory = m.opChoiceFactory();
		this.pp_data = m.ppData();
		this.rparams = m.getConfig().getRelocation();
		this.demand = new ConcurrentHashMap<Id<ActivityFacility>, CarsharingStationDemand>();
	}
	
	void init() {
		HashSet<CarsharingBookingRecord> recs = null;
		if( this.rparams.getEstimated_demand_file() != null ) {
			recs = CarsharingUtils.extractDemandFromBookingFile(this.m, this.rparams.getEstimated_demand_file());
		} else {
			recs = CarsharingUtils.extractDemandFromPlans(this.m);
		}
		for(CarsharingStation s : this.m.getCsScenario().getStations().values()) {
			this.demand.put(s.getId(), new CarsharingStationDemand(s));
		}
		for(CarsharingBookingRecord rec : recs) {
			if(rec.getOriginStation() != null) { 
				this.demand.get(rec.getOriginStation().getId()).push(rec); 
			}
			if(rec.getDestinationStation() != null) {
				this.demand.get(rec.getDestinationStation().getId()).push(rec);
			}
		}
		this.staff_size = this.rparams.getStaff_lbound();
		this.time_bin = this.rparams.getBinstats_lbound();
		this.train_size = this.rparams.getMaxtrain();
		this.time_bin_k_ub = 1+(this.rparams.getBinstats_ubound() - this.rparams.getBinstats_lbound())/STEP;
		this.staff_size_k_ub = 1+(this.rparams.getStaff_ubound() - this.rparams.getStaff_lbound());
		try {
			this.perf_writer = new PrintWriter(new BufferedWriter(new FileWriter(this.rparams.getPerfomance_file(), true)));
			this.traceWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.rparams.getTrace_output_file(), true)));
			this.taskWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.rparams.getTask_output_file(), true)));
			this.traceWriter.println("bin.from\tbin.to\tstation.id\tvalue\tvariable");
			this.taskWriter.println("time\tf.station\tr.station\tn.veh\toperator");
			this.perf_writer.println("iteration\tbin\toperators\tstation.id\tvalue\tvariable");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isActivated() {
		return this.iter >= this.rparams.getActivate_from_iter() && this.iter <= this.rparams.getDeactivate_after_iter();
	}
	
	@Override
	public void updateRelocationList(int time) {
		if(this.time_step.check((int) time)) {
			this.update();
		}
	}
	
	@Override
	public List<CarsharingOffer> relocationList(int time, CarsharingDemand demand, List<CarsharingOffer> offers) {
		if(!this.isActivated()) {
			return new ArrayList<CarsharingOffer>();
		}
		return this.usrelocate(demand, offers);
	}
	
	@Override
	public List<CarsharingRelocationTask> relocationList(int time) {
		List<CarsharingRelocationTask> booked_tasks = new ArrayList<CarsharingRelocationTask>();
		List<CarsharingRelocationTask> tasks = this.oprelocate();
		CarsharingRelocationTask sTask = null;
		int accessTime = 0;
		double accessDistance = 0;
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
					CarsharingOffer off = constructOffer(sTask, t, accessTime, accessDistance);
					ArrayList<CarsharingOffer> offers = new ArrayList<CarsharingOffer>();
					offers.add(off);
					CarsharingBookingRecord b = this.m.booking().process(time_step.step(), off.getDemand(), off, offers);
					if(!b.bookingFailed()) { // **** BOOKING 
						for(CarsharingRelocationTask new_task : temp_tasks) {
							new_task.setBooking(b);
							booked_tasks.add(new_task);
							op.addTask(new_task);
						}
					} else {
						logger.warn("BOOKING FAILURE - " + b.getAgent().getId());
					}
					m.events().processEvent(new CarsharingBookingEvent(time, m.getScenario(), m, b.getDemand(), b));
					sTask = null;
					accessTime = 0;
					accessDistance = 0;
					temp_tasks.clear();
				}
			}
		}
		return booked_tasks;
	}
	
	CarsharingOffer constructOffer(CarsharingRelocationTask sTask, CarsharingRelocationTask eTask, int aTime, double aDist) {
		Leg reloLeg = PopulationUtils.createLeg("RELOCATION");
		Activity reloSrcActivity = PopulationUtils.createActivityFromCoordAndLinkId("STATION", sTask.getStation().facility().getCoord(), sTask.getStation().facility().getLinkId());
		reloSrcActivity.setEndTime(sTask.getTime());
		Activity reloDstActivity = PopulationUtils.createActivityFromCoordAndLinkId("STATION", eTask.getStation().facility().getCoord(), eTask.getStation().facility().getLinkId());
		reloDstActivity.setStartTime(eTask.getTime());
		CarsharingDemand d = new CarsharingDemand(reloLeg, eTask.getAgent(), reloSrcActivity, reloDstActivity, eTask.getSize());
		CarsharingOffer.Builder builder = CarsharingOffer.Builder.newInstanceFromAgent(d.getAgent(), d);
		builder.setAccess(sTask.getTime(), sTask.getStation(), aTime, aDist, CarsharingOffer.OPERATOR_RELOCATION);
		builder.setEgress(eTask.getTime(), eTask.getStation(), 0, 0, CarsharingOffer.OPERATOR_RELOCATION);
		builder.setDrive(eTask.getSize(), eTask.getRoute());
		builder.setCost(0);
		return builder.build();
	}
		
	
	@Override
	public void reset(int iteration) {	
		this.iter = iteration;
		if(!this.isActivated()) return;
		if(this.demand.isEmpty()) init();
		if(iteration > 0) {
			double tot_perf = 0;
			for(CarsharingStationMobsim s : this.m.getStations()) {
				CarsharingBookingStation b = this.m.booking().track(s);
				int dropoff_failed = 0;
				int pickup_failed = 0;
				int dropoff_success = 0;
				int pickup_success = 0;
				for(CarsharingBookingRecord r : b.getDemand(Integer.MAX_VALUE)) {
					if(r.getAgent() instanceof CarsharingOperatorMobsim) continue;
					if(r.bookingFailed()) 
						pickup_failed++;
					else 
						pickup_success++;
				}
				for(CarsharingBookingRecord r : b.getSupply(Integer.MAX_VALUE)) {
					if(r.getAgent() instanceof CarsharingOperatorMobsim) continue;
					if(r.bookingFailed()) 
						dropoff_failed++;
					else 
						dropoff_success++;
				}
				
				double allsum = pickup_success+pickup_failed+dropoff_success+dropoff_failed;
				double successsum = dropoff_success+pickup_success;
				double performance = (allsum == 0)?1:successsum/allsum;
				tot_perf += performance;
				this.perf_writer.println(iteration+"\t"+this.time_bin+"\t"+this.staff_size+"\t"+s.getId().toString()+"\t"+dropoff_failed+"\tDPfailed");
				this.perf_writer.println(iteration+"\t"+this.time_bin+"\t"+this.staff_size+"\t"+s.getId().toString()+"\t"+pickup_failed+"\tPUfailed"); 
				this.perf_writer.println(iteration+"\t"+this.time_bin+"\t"+this.staff_size+"\t"+s.getId().toString()+"\t"+dropoff_success+"\tDPsuccess"); 
				this.perf_writer.println(iteration+"\t"+this.time_bin+"\t"+this.staff_size+"\t"+s.getId().toString()+"\t"+pickup_success+"\tPUsuccess"); 
				this.perf_writer.flush();
			}
			logger.info("performance written : iter " + iteration + " - bin " + this.time_bin + " - tot " + tot_perf);
			//this.time_bin = (this.time_bin + STEP);
			this.time_bin_k = (this.time_bin_k + 1)%this.time_bin_k_ub;
			
			if(this.time_bin >= this.rparams.getBinstats_ubound()) {
				this.staff_size_k = (this.staff_size_k + 1)%this.staff_size_k_ub;
				this.staff_size = this.rparams.getStaff_lbound() + this.staff_size_k;
			}
		}

		List<CarsharingOperatorMobsim> ops = this.m.getOperators().availableSet();
		if(ops.size() != this.staff_size) {
			this.m.getOperators().clear();
			PopulationFactory popFactory = this.m.getScenario().getPopulation().getFactory();
			CarsharingStationMobsim[] stations = this.m.getStations().map().values().toArray(new CarsharingStationMobsim[0]);
			for(int i = 0; i < this.staff_size; i++) {
				this.m.getOperators().add(
						CarsharingOperatorFactory.Builder.
						newInstance(m, popFactory.createPerson(Id.createPersonId("operator_" + i))).
						setChoiceModel(this.choiceFactory).
						setLocation(stations[MatsimRandom.getRandom().nextInt(stations.length)]).
						setTrainSize(this.train_size).
						build());
			}
		} else if(ops.get(0).getMaxRoadtrainSize() != this.train_size) {
			for(CarsharingOperatorMobsim o : this.m.getOperators()) {
				o.setMaxTrainSize(this.train_size);
			}
		}
		
		try {
			this.traceWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.rparams.getTrace_output_file()+"_"+iteration+".log", true)));
			this.taskWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.rparams.getTask_output_file()+"_"+iteration+".log", true)));
			this.traceWriter.println("time\tstation.id\tvalue\tvariable");
			this.taskWriter.println("time\tf.station\tr.station\tn.veh\toperator");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.time_bin = this.rparams.getBinstats_lbound() + this.time_bin_k*STEP;
		this.time_step = new TimeStep(0, this.time_bin);
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
