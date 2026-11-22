package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

import java.math.BigDecimal;
import java.util.Objects;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

/**
 * STORY 5.1: Configuration Review
 *
 * Represents a comparison between a baseline fare
 * and a staged PostgreSQL fare draft.
 *
 * Supports both O-D and zonal fare configurations.
 */
public record ConfigurationChangeView(
    String tenantCode,
    GridMode gridMode,
    String selectedContext,
    String rowKey,
    String colKey,
    String product,
    BigDecimal publishedPrice,
    BigDecimal stagedPrice
) {

    // Returns true when the proposed price differs from the baseline.
    public boolean isChanged() {
        if (publishedPrice == null || stagedPrice == null) {
            return !Objects.equals(publishedPrice, stagedPrice);
        }

        return publishedPrice.compareTo(stagedPrice) != 0;
    }
}

