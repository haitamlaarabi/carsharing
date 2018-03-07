package org.matsim.contrib.gcs.replanning.algos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.replanning.CarsharingPlanModeCst;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

public class CarsharingSubTourPermissableModesCalculator implements PermissibleModesCalculator {

	private final Scenario scenario;
	private final List<String> availableModes;
	private final List<String> availableModesWithoutCar;
	private final CarsharingManager manager;
	
	public CarsharingSubTourPermissableModesCalculator(Scenario scenario, final CarsharingManager manager, final String[] availableModes) {
		this.manager = manager;
		this.scenario = scenario;
		this.availableModes = Arrays.asList(availableModes);
		
		if ( this.availableModes.contains(TransportMode.car) )  {
			final List<String> l = new ArrayList<String>( this.availableModes );
			while ( l.remove( TransportMode.car ) ) 
			{
				// removing all cars
			}
			this.availableModesWithoutCar = Collections.unmodifiableList( l );
		}
		else {
			this.availableModesWithoutCar = this.availableModes;
		}
		
	}
	
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {

		List<String> l; 

		final boolean carAvail = !"no".equals( PersonUtils.getLicense(plan.getPerson()) ) &&	!"never".equals( PersonUtils.getCarAvail(plan.getPerson()) );
		if (carAvail)			 
			  l = new ArrayList<String>( this.availableModes );
		  else
			  l = new ArrayList<String>( this.availableModesWithoutCar );
		
		CarsharingCustomerMobsim customer = this.manager.customers().map().get(plan.getPerson().getId());
		if(customer != null) {
			l.add(CarsharingPlanModeCst.directTrip);
		} 

		return l;
	}

}
