package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;

/*Retrieves the current baseline price for a staged fare.
*/

public interface FareBaselineProvider {

    BigDecimal getBaselinePrice(FareDraft draft);

}