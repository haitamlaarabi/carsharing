package org.matsim.haitam.api.operation.impl;

import org.matsim.haitam.api.operation.model.CarsharingPowerSourceModel;

public class CarsharingPowerSourceModelImpl implements CarsharingPowerSourceModel {
	
	final double power;
	
	public CarsharingPowerSourceModelImpl(double power) {
		this.power = power;
	}

	@Override
	public double getPower(double time) {
		return this.power;
	}

	@Override
	public String getName() {
		return "PowerSource";
	}

}
