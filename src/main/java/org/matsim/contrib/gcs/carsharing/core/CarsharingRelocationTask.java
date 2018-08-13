package org.matsim.contrib.gcs.carsharing.core;

import org.matsim.contrib.gcs.router.CarsharingRouterUtils.RouteData;

public class CarsharingRelocationTask implements Comparable<CarsharingRelocationTask> {
	
	private String type;
	final private CarsharingAgent agent;
	final private CarsharingStationMobsim station;
	final private int time;
	private int size;
	final private String id;
	final private double distance;
	final private int tt;
	private CarsharingBookingRecord booking = null;
	private RouteData route = null;
	private String comment = null;
	
	public static CarsharingRelocationTask startTask(String id, int time, CarsharingAgent agent, CarsharingStationMobsim station, int size, int tt, double distance) {
		CarsharingRelocationTask t = new CarsharingRelocationTask(id, time, agent, station, size, tt, distance);
		t.type = "START";
		return t;
	}
	
	public static CarsharingRelocationTask endTask(String id, int time, CarsharingAgent agent, CarsharingStationMobsim station, int size, int tt, double distance) {
		CarsharingRelocationTask t = new CarsharingRelocationTask(id, time, agent, station, size, tt, distance);
		t.type = "END";
		return t;
	}
		
	private CarsharingRelocationTask(String id, int time, CarsharingAgent agent, CarsharingStationMobsim station, int size, int tt, double distance) {
		this.agent = agent;
		this.station = station;
		this.size = size;
		this.time = time;
		this.id = id;
		this.distance = distance;
		this.tt = tt;
	}

	public String getId() {
		return id;
	}
	
	public String getType() {
		return type;
	}
	
	public int getTravelTime() {
		return this.tt;
	}
	
	public double getDistance() {
		return distance;
	}

	public CarsharingAgent getAgent() {
		return agent;
	}

	public CarsharingStationMobsim getStation() {
		return station;
	}
	
	public int getTime() {
		return this.time;
	}

	public int getSize() {
		return size;
	}
	
	public void setSize(int s) {
		this.size = s;
	}
	
	public void setBooking(CarsharingBookingRecord b) {
		this.booking = b;
	}

	public CarsharingBookingRecord getBooking() {
		return this.booking;
	}

	public void setRoute(RouteData route) {
		this.route = route;
	}
	
	public RouteData getRoute() {
		return this.route;
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Override
	public int compareTo(CarsharingRelocationTask o) {
		return Double.compare(this.getTime(), o.getTime());
	}

	

}
