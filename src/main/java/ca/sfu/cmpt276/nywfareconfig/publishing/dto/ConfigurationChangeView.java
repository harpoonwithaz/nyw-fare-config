package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * STORY 5.1: Configuration Review
 *
 * Represents one fare configuration comparison.
 * Supports both O-D and zonal fare grids.
 *
 * Currently used with mock data.
 * Later, values will be retrieved from PostgreSQL.
 */
public record ConfigurationChangeView(
    String tenantCode,
    String gridMode,
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

