package org.matsim.contrib.gcs.carsharing.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationPowerController;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStations;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicle;
import org.matsim.contrib.gcs.operation.model.CarsharingParkingModel;
import org.matsim.contrib.gcs.router.CarsharingRouterUtils;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class CarsharingStationFactory {

	public static CarsharingStationBuilder stationBuilder(Scenario scenario, String id, Coord coord) {
		return new CarsharingStationBuilder(scenario, id, coord);
	}
	
	public static CarsharingStationMobsimBuilder stationMobsimBuilder(CarsharingStation station) {
		return new CarsharingStationMobsimBuilder(station);
	}	
	
	public static CarsharingStationMobsim getStationCopy(CarsharingStationMobsim s) {
		CarsharingStationImpl station = new CarsharingStationImpl(s.facility());
		station.setCapacity(s.getCapacity());
		station.setType(s.getType());
		station.setName(s.getName());
		for(Entry<Id<Vehicle>, CarsharingVehicle> e : s.initialFleet().entrySet()) {
			station.initialFleet().put(e.getKey(), e.getValue());
		}
		station.parking = s.parking();
		return station;
	}
	
	public static CarsharingStations stations(final Network network) {
		
		return new CarsharingStations() {
			@Inject private QuadTree<CarsharingStationMobsim> stationstree = new QuadTreeFactory<CarsharingStationMobsim>(network).get();
			private final Map<Id, CarsharingStationMobsim> stationsmap = new HashMap<Id, CarsharingStationMobsim>();
			@Override
			public QuadTree<CarsharingStationMobsim> qtree() {
				return stationstree;
			}
			@Override
			public Map<Id, CarsharingStationMobsim> map() {
				return Collections.unmodifiableMap(stationsmap);
			}
			@Override
			public void add(CarsharingStationMobsim station) {
				if(!stationsmap.containsKey(station.facility().getId())) {
					stationsmap.put(station.facility().getId(), station);
					stationstree.put(station.facility().getCoord().getX(), station.facility().getCoord().getY(), station);
				} else {
					throw new RuntimeException("station with id " + station.facility().getId() + " already exist!");
				}
			}
			@Override
			public int size() {
				return stationsmap.size();
			}
			@Override
			public Iterator<CarsharingStationMobsim> iterator() {
				return this.stationsmap.values().iterator();
			}
			@Override
			public void clear() {
				stationstree.clear();
				stationsmap.clear();
			}
		};
		
	}
	
	
	public static class CarsharingStationBuilder {
		final CarsharingStationImpl station;
		static Network carNetwork = null;
		
		private CarsharingStationBuilder(Scenario scenario, String id, Coord coord) {
			if(carNetwork == null) {
				carNetwork = NetworkUtils.createNetwork();		
				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
				Set<String> modes = new HashSet<>();
				modes.add(TransportMode.car);
				filter.filter(carNetwork, modes);
			}
			
			Link link = NetworkUtils.getNearestLink(carNetwork, coord);
			Id<ActivityFacility> idf = Id.create(id, ActivityFacility.class);
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(idf);
			if(facility == null) {
				facility = scenario.getActivityFacilities().getFactory().
						createActivityFacility(idf, coord, link.getId());
				ActivityOption actopt = scenario.getActivityFacilities().getFactory().
						createActivityOption(CarsharingRouterUtils.ACTIVITY_TYPE_NAME);
				actopt.addOpeningTime(
						new OpeningTimeImpl(CarsharingUtils.toSecond(0,0,0), CarsharingUtils.toSecond(23,59,59)));
				facility.addActivityOption(actopt);
				scenario.getActivityFacilities().addActivityFacility(facility);
			}
			
			this.station = new CarsharingStationImpl(facility);
			station.setType("Default");
		}
		
		public CarsharingStationBuilder setName(String name){
			station.setName(name);
			return this;
		}
		public CarsharingStationBuilder setType(String type) {
			station.setType(type);
			return this;
		}
		public CarsharingStationBuilder setCapacity(int capacity) {
			station.setCapacity(capacity);
			return this;
		}
		public CarsharingStation build(){
			return this.station;
		}
	}
	
	public static class CarsharingStationMobsimBuilder {
		CarsharingStationImpl station;

		private CarsharingStationMobsimBuilder(CarsharingStation station) {
			this.station = (CarsharingStationImpl)station;
		}
		public CarsharingStationMobsimBuilder setPowerController(CarsharingStationPowerController powercontroller) {
			station.powerController = powercontroller;
			return this;
		}
		public CarsharingStationMobsimBuilder setParkingModel(CarsharingParkingModel parkingmodel) {
			station.parking = parkingmodel;
			parkingmodel.setCapactiy(station.getCapacity());
			return this;
		}
		public CarsharingStationMobsim build(Scenario scenario) {
			return station;
		}
	}
	
}
