package ca.sfu.cmpt276.nywfareconfig.fare.dto;

public record StationZoneMappingView(
        String siteCode,
        String stationName,
        String zoneCode,
        String zoneDisplayName) {
}