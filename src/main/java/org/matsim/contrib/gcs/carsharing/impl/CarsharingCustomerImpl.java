package org.matsim.contrib.gcs.carsharing.impl;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.core.CarsharingBookingRecord;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;

public class CarsharingCustomerImpl implements CarsharingCustomerMobsim {
	
	private class CarsharingCustomerStatusImpl implements CarsharingCustomerStatus {
		private PlanElement planElement = null;
		private CarsharingBookingRecord bookRecord = null;
		@Override public PlanElement getPlanElement() { return planElement; }
		@Override public CarsharingBookingRecord getOngoingRental() { return this.bookRecord; }
		@Override public void setPlanElement(PlanElement planElement) { this.planElement = planElement; }
		@Override public void setOngoingRental(CarsharingBookingRecord bookrec) { this.bookRecord = bookrec; }
	}
	
	
	// ********************

	protected final Person person;
	private CarsharingUserChoiceModel decisionEngine;
	private CarsharingCustomerStatus status;
	
	
	public CarsharingCustomerImpl(Person person, CarsharingUserChoiceModel userChoice) {
		this.decisionEngine = null;
		this.person = person;
		this.status = new CarsharingCustomerStatusImpl();
		this.decisionEngine = userChoice;
		this.decisionEngine.bindTo(this);
	}
	

	@Override
	public void reset(int iteration) {
		this.status = new CarsharingCustomerStatusImpl();
	}
	@Override
	public String getId() {	
		return person.getId().toString(); 
	}
	@Override
	public Person getPerson() { 
		return this.person; 
	}
	@Override
	public void setStatus(PlanElement pe, CarsharingBookingRecord bookrec) {
		((CarsharingCustomerStatusImpl)this.status).bookRecord = bookrec;
		((CarsharingCustomerStatusImpl)this.status).planElement = pe;
	}
	@Override
	public CarsharingUserChoiceModel decision() {
		return this.decisionEngine;
	}
	@Override
	public CarsharingCustomerStatus status() { 
		return this.status; 
	}

}
