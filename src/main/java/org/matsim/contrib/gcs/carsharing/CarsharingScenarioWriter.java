package org.matsim.contrib.gcs.carsharing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStation;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingVehicle;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

public class CarsharingScenarioWriter extends MatsimXmlWriter {
	
	private static Logger logger = Logger.getLogger(CarsharingScenarioWriter.class);
	private CarsharingScenario cs = null;
	private CarsharingManager m = null;
	private StringBuffer stationsXML;
	private StringBuffer vehiclesXML;
	private final Map<Id<Vehicle>, Id<ActivityFacility>> stationVehicle;
	
	CarsharingScenarioWriter() {
		super();
		this.stationsXML = new StringBuffer("");
		this.vehiclesXML = new StringBuffer("");
		this.stationVehicle = new HashMap<Id<Vehicle>, Id<ActivityFacility>>();
	}
	
	public CarsharingScenarioWriter(CarsharingManager manager) {
		this();
		this.m = manager;
	}
	
	public CarsharingScenarioWriter(CarsharingScenario carsharing) {
		this();
		this.cs = carsharing;
	}
	
	
	
	public void writeXml(String filename) {
		// Write from scenario
		if(this.cs != null) {
			for (CarsharingStation station : this.cs.getStations().values()) {
				appendStation(station);
				for(CarsharingVehicle v : station.initialFleet().values()) {
					this.stationVehicle.put(v.vehicle().getId(), station.facility().getId());
				}
			}
			for(CarsharingVehicle vehicle : this.cs.getVehicles().values()) {
				appendVehicle(vehicle);
			}
		}
		// Write from manager
		if(this.m != null) {
			for (CarsharingStationMobsim station : this.m.getStations()) {
				appendStation(station);
				for(CarsharingVehicle v : station.parking()) {
					this.stationVehicle.put(v.vehicle().getId(), station.facility().getId());
				}
			}
			for(CarsharingVehicle vehicle : this.m.vehicles()) {
				appendVehicle(vehicle);
			}
		}
		writeAll(filename);
	}
	
	
	private void writeAll(String filename) {
		try {
			logger.info("write car sharing infratructure");
			openFile(filename);
			writeXmlHead();
			writer.write("<" + CarsharingScenarioReader.CARSHARING_SCENARIO + ">\n");
			this.writeStations();
			this.writeVehicles();
			writer.write("</" + CarsharingScenarioReader.CARSHARING_SCENARIO + ">\n");
			close();
			logger.info("done");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeStations() throws IOException {
		this.writer.write("\t<" + CarsharingScenarioReader.STATIONS + ">\n");
		this.writer.write(this.stationsXML.toString());
		this.writer.write("\t</" + CarsharingScenarioReader.STATIONS + ">\n");
	}
	
	private void writeVehicles() throws IOException {
		this.writer.write("\t<" + CarsharingScenarioReader.VEHICLES + ">\n");
		this.writer.write(this.vehiclesXML.toString());
		this.writer.write("\t</" + CarsharingScenarioReader.VEHICLES + ">\n");
	}

	
	private String setTagIdClassName(String tag, String id, String classname) {
		return "\t\t<" + tag  + "  " + 
				CarsharingScenarioReader.ID + "=\"" + id + "\"  " + 
				CarsharingScenarioReader.CLASSNAME + "=\"" + classname + "\"/>\n";
	}


	private void appendStation(CarsharingStation station) {
		this.stationsXML.append("\t\t<" + 
				CarsharingScenarioReader.STATION + "  " + 
				CarsharingScenarioReader.ID + "=\"" + station.facility().getId() + "\" " + 
				CarsharingScenarioReader.NAME + "=\"" + station.getName() + "\" " + 
				CarsharingScenarioReader.X + "=\"" + station.facility().getCoord().getX() + "\" " + 
				CarsharingScenarioReader.Y + "=\"" + station.facility().getCoord().getY() + "\" " + 
				CarsharingScenarioReader.CAPACITY + "=\"" + station.getCapacity() + "\" " + 
				CarsharingScenarioReader.STATIONTYPE + "=\"" + station.getType()  + "\"/>\n");
	}
	

	private void appendVehicle(CarsharingVehicle carsharing) {
		this.vehiclesXML.append("\t\t<" + 
				CarsharingScenarioReader.VEHICLE + "  " + 
				CarsharingScenarioReader.ID + "=\"" + carsharing.vehicle().getId() + "\" " + 
				CarsharingScenarioReader.NAME + "=\"" + carsharing.getName() + "\" " + 
				CarsharingScenarioReader.STATION + "=\"" + this.stationVehicle.get(carsharing.vehicle().getId()) + "\" " + 
				CarsharingScenarioReader.VEHICLETYPE + "=\"" + carsharing.getType() + "\"/>\n"); 
	}
	

}
