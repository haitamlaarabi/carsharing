package org.matsim.haitam.api.carsharing.core;

import java.util.Map;

import org.matsim.api.core.v01.Id;

public interface GenericContainer<T> extends Iterable<T> {

	Map<Id, T> map();
	void add(final T t);
	int size();
	void clear();
	
}
