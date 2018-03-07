package org.matsim.contrib.gcs.carsharing.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomers;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;

public class CarsharingCustomerFactory {

	public static CarsharingCustomerMobsim createCustomer(Person p, CarsharingUserChoiceModel userChoice) {
		return new CarsharingCustomerImpl(p, userChoice);
	}
	
	public static CarsharingCustomers customers() {
		return new CarsharingCustomers() {
			private final HashMap<Id, CarsharingCustomerMobsim> customers = new HashMap<Id, CarsharingCustomerMobsim>();
			@Override
			public Map<Id, CarsharingCustomerMobsim> map() {
				return Collections.unmodifiableMap(customers);
			}
			@Override
			public void add(CarsharingCustomerMobsim customer) {
				customers.put(customer.getPerson().getId(), customer);
			}
			@Override
			public int size() {
				return customers.size();
			}
			@Override
			public Iterator<CarsharingCustomerMobsim> iterator() {
				return customers.values().iterator();
			}
			@Override
			public void clear() {
				customers.clear();
			}
		};
	}
	
	
}
