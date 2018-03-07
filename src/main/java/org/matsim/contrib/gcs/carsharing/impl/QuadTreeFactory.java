package org.matsim.contrib.gcs.carsharing.impl;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class QuadTreeFactory<T> implements Provider<QuadTree<T>> {

	@Inject private Network network;
	
	public QuadTreeFactory(final Network network) {
		this.network = network;
	}
	
	/*public QuadTree<T> fill(Collection<T> collections) {
		QuadTree<T> qt = get();
		for(T t : collections) {
			qt.put(t.getCoordinate().getX(), t.getCoordinate().getY(), t);
		}
		return qt;
	}*/
	
	@Override
	public QuadTree<T> get() {
		double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);
	    
        for (Link l : network.getLinks().values()) {
  	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
  	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
  	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
  	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
  	    }
  	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
		
		return new QuadTree<T>(minx, miny, maxx, maxy);
	}

}
