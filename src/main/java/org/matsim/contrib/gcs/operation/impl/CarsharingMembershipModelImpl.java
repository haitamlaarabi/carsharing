package org.matsim.contrib.gcs.operation.impl;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.carsharing.CarsharingManager;
import org.matsim.contrib.gcs.carsharing.core.CarsharingCustomerMobsim;
import org.matsim.contrib.gcs.carsharing.core.CarsharingStationMobsim;
import org.matsim.contrib.gcs.carsharing.impl.CarsharingCustomerFactory;
import org.matsim.contrib.gcs.operation.model.CarsharingMembershipModel;
import org.matsim.contrib.gcs.operation.model.CarsharingUserChoiceModel;
import org.matsim.contrib.gcs.router.CarsharingRouterModeCst;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.PtConstants;

public class CarsharingMembershipModelImpl implements CarsharingMembershipModel {

	static String carsharingNearBy = "CS:nearby";
	final QuadTree<CarsharingStationMobsim> qt;
	final Double searchDistance;
	final boolean distanceBased;
	final CarsharingManager m;
	
	public CarsharingMembershipModelImpl(CarsharingManager manager, boolean distanceBased) {
		this.m = manager;
		this.qt = manager.getStations().qtree();
		this.searchDistance = manager.getConfig().getSearchDistance();
		this.distanceBased = distanceBased;
	}
	
	@Override
	public CarsharingCustomerMobsim checkin(Person p, CarsharingUserChoiceModel decision) {
		boolean goodcustomer = false;
		
		// Check in customer, only if all activites are located near by a carsharing station
		QuadTree<CarsharingStationMobsim> qt = m.getStations().qtree();
		int counter = 0;
		int nearstationcounter = 0;
		boolean alreadyUsingCarsharing = false;
		for(PlanElement pe : p.getSelectedPlan().getPlanElements()) {
			if(pe instanceof Activity) {
				Activity act = ((Activity)pe);
				if(checkNotStageActivity(act)) {
					if(qt.getDisk(act.getCoord().getX(), act.getCoord().getY(), this.searchDistance).size() >= 1) {
						nearstationcounter++;
					}
					counter++;
				}
			} else if(CarsharingUtils.isCarsharingElement((Leg)pe)) {
				alreadyUsingCarsharing = true;
			}
		}
		if(this.distanceBased && (counter > 0 && nearstationcounter == counter)) {
			goodcustomer = true;
		} else if(alreadyUsingCarsharing) {
			goodcustomer = true;
		}

		if(goodcustomer) {
			CarsharingCustomerMobsim newCustomer = CarsharingCustomerFactory.createCustomer(p, decision);
			m.customers().add(newCustomer);
			return newCustomer;
		}
		return null;
	}
		
	private boolean checkNotStageActivity(Activity act) {
		ActivityFacility af = m.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId());
		if(af == null)
			return true;
		
		if(af != null && 
				(!act.getType().equals(CarsharingRouterModeCst.ACTIVITY_TYPE_NAME) && 
				!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))) {
				return true;
		} 
	
		return false;
	}
}
