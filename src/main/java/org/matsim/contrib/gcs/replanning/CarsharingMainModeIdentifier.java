package org.matsim.contrib.gcs.replanning;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.gcs.utils.CarsharingUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;

public class CarsharingMainModeIdentifier implements MainModeIdentifier {

	final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();
	
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		for ( PlanElement pe : tripElements ) {
        	if ( pe instanceof Leg && CarsharingUtils.isUnRoutedCarsharingLeg(pe) ) {
                return ((Leg) pe).getMode();
            }
        }
        return defaultModeIdentifier.identifyMainMode( tripElements );
	}

}
