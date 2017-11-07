package org.matsim.haitam.api.carsharing.core;

import org.matsim.core.utils.collections.QuadTree;

public interface GeoContainer<T> extends GenericContainer<T> {

	QuadTree<T> qtree();
	
}
