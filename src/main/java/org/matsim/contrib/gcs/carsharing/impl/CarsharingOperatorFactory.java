package org.matsim.contrib.gcs.carsharing.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperatorMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingOperators;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.operation.model.CarsharingOperatorChoiceModel;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CarsharingOperatorFactory {

	public static class Builder {
		CarsharingOperatorImpl operator;
		private Builder(Person p) {
			operator = new CarsharingOperatorImpl(p);
		}
		public static Builder newInstance(CarsharingManager m, Person p) {
			return new Builder(p);
		}
		public Builder setTrainSize(int size) {
			operator.trainSize = size;
			return this;
		}
		public Builder setLocation(CarsharingStationMobsim location) {
			operator.initialLocation = location;
			return this;
		}
		public Builder setChoiceModel(CarsharingOperatorChoiceModel decision) {
			operator.decisionEngine = decision;
			operator.decisionEngine.bindTo(operator);
			return this;
		}
		public Builder setChoiceModel(Provider<CarsharingOperatorChoiceModel> decisionFactory) {
			operator.decisionEngine = decisionFactory.get();
			operator.decisionEngine.bindTo(operator);
			return this;
		}
		public CarsharingOperatorMobsim build() {
			operator.reset(0);
			return operator;
		}
	}
	/*public static CarsharingOperatorMobsim createOperator(Person p, int maxroadtrainsize, CarsharingStationMobsim location, CarsharingOperatorChoiceModel decision) {
		return new CarsharingOperatorImpl(p, maxroadtrainsize, location, decision);
	}*/
	
	public static CarsharingOperators operators(final Network network) {
		return new CarsharingOperators() {
			@Inject private QuadTree<CarsharingOperatorMobsim> operatorstree = new QuadTreeFactory<CarsharingOperatorMobsim>(network).get();
			private final Map<Id, CarsharingOperatorMobsim> operatorsmap = new HashMap<Id, CarsharingOperatorMobsim>();
			@Override
			public QuadTree<CarsharingOperatorMobsim> qtree() {
				return operatorstree;
			}
			@Override
			public Map<Id, CarsharingOperatorMobsim> map() {
				return Collections.unmodifiableMap(operatorsmap);
			}
			@Override
			public void add(CarsharingOperatorMobsim operator) {
				operatorsmap.put(operator.getPerson().getId(), operator);
				Coord coord = operator.getLocation().facility().getCoord();
				operatorstree.remove(coord.getX(), coord.getY(), operator);
				operatorstree.put(coord.getX(), coord.getY(), operator);
			}
			@Override
			public int size() {
				return operatorsmap.size();
			}
			@Override
			public Iterator<CarsharingOperatorMobsim> iterator() {
				return this.operatorsmap.values().iterator();
			}
			@Override
			public void clear() {
				operatorsmap.clear();
				operatorstree.clear();
			}
			@Override
			public List<CarsharingOperatorMobsim> availableSet() {
				List<CarsharingOperatorMobsim> ops = new LinkedList<CarsharingOperatorMobsim>();
				for(CarsharingOperatorMobsim op : operatorsmap.values()) {
					if(op.available()) {
						ops.add(op);
					}
				}
				return ops;
			}
		};
	}
}
