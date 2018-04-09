package org.matsim.contrib.gcs.config;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

public class CarsharingRelocationParams extends ReflectiveConfigGroup implements MatsimParameters {
	
	final static String SET_TYPE = "carsharingRelocationParams";


	private int activate_from_iter = 0;
	private int deactivate_after_iter = 0;
	private int binstats_lbound = 1800;
	private int binstats_ubound = 1800;
	//private int statsbin_step = 0;
	private int staff_lbound = 1;
	private int staff_ubound = 1;
	private int maxtrain =1;



	private String perfomance_file = null;
	private String task_output_file = null;
	private String trace_output_file = null;
	private String estimated_demand_file = null;



	CarsharingRelocationParams() {
		super( SET_TYPE );
	}
	
	/**
	 * @return the maxtrain
	 */
	public int getMaxtrain() {
		return maxtrain;
	}


	/**
	 * @param maxtrain the maxtrain to set
	 */
	public void setMaxtrain(int maxtrain) {
		this.maxtrain = maxtrain;
	}
	
	/**
	 * @return the task_file
	 */
	public String getTask_output_file() {
		return task_output_file;
	}

	/**
	 * @param task_file the task_file to set
	 */
	public void setTask_output_file(String task_file) {
		this.task_output_file = task_file;
	}

	/**
	 * @return the trace_file
	 */
	public String getTrace_output_file() {
		return trace_output_file;
	}

	/**
	 * @param trace_file the trace_file to set
	 */
	public void setTrace_output_file(String trace_file) {
		this.trace_output_file = trace_file;
	}
	
	/**
	 * @return the activate_from_iter
	 */
	public int getActivate_from_iter() {
		return activate_from_iter;
	}


	/**
	 * @param activate_from_iter the activate_from_iter to set
	 */
	public void setActivate_from_iter(int activate_from_iter) {
		this.activate_from_iter = activate_from_iter;
	}


	/**
	 * @return the deactivate_after_iter
	 */
	public int getDeactivate_after_iter() {
		return deactivate_after_iter;
	}


	/**
	 * @param deactivate_after_iter the deactivate_after_iter to set
	 */
	public void setDeactivate_after_iter(int deactivate_after_iter) {
		this.deactivate_after_iter = deactivate_after_iter;
	}


	/**
	 * @return the lower_bound_bin
	 */
	public int getBinstats_lbound() {
		return binstats_lbound;
	}


	/**
	 * @param lower_bound_bin the lower_bound_bin to set
	 */
	public void setBinstats_lbound(int lower_bound_bin) {
		this.binstats_lbound = lower_bound_bin;
	}


	/**
	 * @return the upper_bound_bin
	 */
	public int getBinstats_ubound() {
		return binstats_ubound;
	}


	/**
	 * @param upper_bound_bin the upper_bound_bin to set
	 */
	public void setBinstats_ubound(int upper_bound_bin) {
		this.binstats_ubound = upper_bound_bin;
	}


	/**
	 * @return the lower_bound_staff
	 */
	public int getStaff_lbound() {
		return staff_lbound;
	}


	/**
	 * @param lower_bound_staff the lower_bound_staff to set
	 */
	public void setStaff_lbound(int lower_bound_staff) {
		this.staff_lbound = lower_bound_staff;
	}


	/**
	 * @return the upper_bound_staff
	 */
	public int getStaff_ubound() {
		return staff_ubound;
	}


	/**
	 * @param upper_bound_staff the upper_bound_staff to set
	 */
	public void setStaff_ubound(int upper_bound_staff) {
		this.staff_ubound = upper_bound_staff;
	}


	/**
	 * @return the perfomance_file
	 */
	public String getPerfomance_file() {
		return perfomance_file;
	}


	/**
	 * @param perfomance_file the perfomance_file to set
	 */
	public void setPerfomance_file(String perfomance_file) {
		this.perfomance_file = perfomance_file;
	}


	/**
	 * @return the estimated_demand_file
	 */
	public String getEstimated_demand_file() {
		return estimated_demand_file;
	}


	/**
	 * @param estimated_demand_file the estimated_demand_file to set
	 */
	public void setEstimated_demand_file(String estimated_demand_file) {
		this.estimated_demand_file = estimated_demand_file;
	}
}
