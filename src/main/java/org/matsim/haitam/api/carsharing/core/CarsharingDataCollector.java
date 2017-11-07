package org.matsim.haitam.api.carsharing.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;


public class CarsharingDataCollector {

	public interface CarsharingDataProvider {
		Collection<Map<String, String>> getLogRows(double time);
		String getLogFile();
		String getLogType();
		String getModuleName();
		void reset(int iteration);
	}
	
	private Map<String, DataLog> logDataMap = new HashMap<String, DataLog>();
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	private final Map<String, CarsharingDataProvider> analysisMap = new HashMap<String, CarsharingDataProvider>();
	private HashMap<Class<? extends CarsharingDataProvider>, String> warningStop = new HashMap<Class<? extends CarsharingDataProvider>, String>();
	
	
	public void reset(int iteration) {
		for(CarsharingDataProvider data : this.analysisMap.values()) {
			data.reset(iteration);
		}
		for(DataLog data: logDataMap.values()) {
			data.clear();
		}
		this.warningStop.clear();
	}
	
	private void addAllModuleLog() {
		for(CarsharingDataProvider data: this.getAllModules()) {
			this.addLog(data, Double.NaN);
		}
	}
	

	public void addLog(CarsharingDataProvider logging, double time) {
		if(logging == null) {
			logger.warning("data provider is null! no logs are added - time [" + time + "]");
		} else if (logging.getLogFile() == null) { 
			logger.warning("data provider file is null! no logs are added - time [" + time + "] - class [" + logging.getClass().getSimpleName() + "]");
		} else {
			if(!logDataMap.containsKey(logging.getLogType())) {
				logDataMap.put(logging.getLogType(), new DataLog(logging.getLogFile()));
			}
			Collection<Map<String, String>> logs = logging.getLogRows(time);
			if(logs != null) {
				for(Map<String, String> log : logs) {
					logDataMap.get(logging.getLogType()).add(log);
				}
			} else if(this.warningStop.get(logging.getClass()) == null) {
				this.warningStop.put(logging.getClass(), "Last warning, data provider Okay, but no data collected! for class [" + logging.getClass() + "] !");
				logger.warning(this.warningStop.get(logging.getClass()));
			}
		}
	}
	
	
	public void writeLogs(int iteration) {
		addAllModuleLog();
		Iterator<DataLog> itr = logDataMap.values().iterator();
		while(itr.hasNext()) {
			DataLog temp = itr.next();
			if(!temp.isEmpty())
				temp.writeToFile(iteration);
		}
	}
	

	public CarsharingDataProvider getModule(String moduleName) {
		return this.analysisMap.get(moduleName);
	}

	
	public void addModule(CarsharingDataProvider module) {
		this.analysisMap.put(module.getModuleName(), module);
	}
	
	public void addAllModule(Collection<CarsharingDataProvider> modules) {
		for(CarsharingDataProvider m : modules) {
			this.analysisMap.put(m.getModuleName(), m);
		}
	}

	
	public Collection<CarsharingDataProvider> getAllModules() {
		return this.analysisMap.values();
	}

	
	// ********************************************************************
	// Generic Data Logging Class
	// ********************************************************************
	/**
	 * 
	 * @author haitam
	 *
	 */
	public static class DataLog {
		private String fileName;
		private ArrayList<String> headerRowList;
		private ArrayList<String> dataStrRows;
		
		public DataLog(String fileName) { 
			this.fileName = fileName;
			headerRowList = new ArrayList<String>();
			dataStrRows = new ArrayList<String>();
		}
		
		public ArrayList<String> getHeaderRowList() { return headerRowList; }
		public ArrayList<String> getDataRowStr() { return dataStrRows; }
		
		private String getHeaderRowAsString() {
			Iterator<String> itr = this.headerRowList.iterator();
			String headerRowStr = "";
			while(itr.hasNext()) {
				if(!headerRowStr.isEmpty()) headerRowStr += "\t";
				headerRowStr += itr.next();
			}
			return headerRowStr;
		}
		
		private void extractHeaderRow(Map<String, String> data) {
			Iterator<Entry<String,String>> it = data.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String,String> entry = it.next();
				if(!headerRowList.contains(entry.getKey())) {
					headerRowList.add(entry.getKey());
				}
			}
		}
		
		public void add(Map<String, String> data) {
			if(data.size() > this.headerRowList.size()) {
				this.extractHeaderRow(data);
			}
			String daraRowStr = "";
			for(int i = 0; i < headerRowList.size(); i++) {
				if(!daraRowStr.isEmpty()) daraRowStr += "\t";
				daraRowStr += String.valueOf(data.get(headerRowList.get(i)));
			}
			dataStrRows.add(daraRowStr);
		}
		
		public void writeToFile(int iteration) {	
			dataStrRows.add(0, getHeaderRowAsString());
			GeneralLib.writeList(dataStrRows, fileName + "_" + iteration + "_.log");
		}
		
		public void clear() {
			headerRowList.clear();
			dataStrRows.clear();
		}
		
		public boolean isEmpty() {
			return dataStrRows.isEmpty();
		}
		
	}


}
