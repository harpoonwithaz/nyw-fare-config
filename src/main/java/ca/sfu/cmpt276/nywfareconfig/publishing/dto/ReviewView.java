package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

public record ReviewView(
    String ticketType,
    double publishedPrice,
    double draftPrice
) {
    public boolean isChanged() {
        return Double.compare(publishedPrice, draftPrice) != 0;
    }
}

