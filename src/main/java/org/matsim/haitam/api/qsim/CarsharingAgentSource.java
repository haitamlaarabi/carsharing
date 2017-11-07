package org.matsim.haitam.api.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.haitam.api.carsharing.CarsharingManager;
import org.matsim.haitam.api.carsharing.CarsharingScenario;
import org.matsim.haitam.api.carsharing.core.CarsharingStationMobsim;
import org.matsim.haitam.api.carsharing.core.CarsharingVehicleMobsim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


public class CarsharingAgentSource implements AgentSource {

	protected final QSim qsim;
	protected final CarsharingManager manager;
	private Collection<MobsimAgent> mobSimAgents;
	private Collection<Vehicle> mobSimVehicles;
	private AgentFactory agentFactory;

	public CarsharingAgentSource(
						AgentFactory agentFactory, 
						QSim qsim,
						CarsharingManager manager) {
		
		this.qsim = qsim; 
		this.agentFactory = agentFactory;
		this.manager = manager;
		mobSimAgents = new ArrayList<MobsimAgent>();
		mobSimVehicles = new ArrayList<Vehicle>();
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		for (CarsharingStationMobsim station: manager.getStations()) {
			for(CarsharingVehicleMobsim csVehicle : station.parking()) {
				qsim.createAndParkVehicleOnLink(csVehicle.vehicle(), station.facility().getLinkId());
				mobSimVehicles.add(csVehicle.vehicle());
			}
		}
	}
	
	
	public Collection<MobsimAgent> getMobSimAgents() {
		return mobSimAgents;
	}
	
	public Collection<Vehicle> getMobSimVehicles() {
		return mobSimVehicles;
	}
	
	
}
