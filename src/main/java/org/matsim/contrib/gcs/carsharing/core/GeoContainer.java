package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.core.utils.collections.QuadTree;

public interface GeoContainer<T> extends GenericContainer<T> {

	QuadTree<T> qtree();
	
}
