package ca.sfu.cmpt276.nywfareconfig.alert.dto;

public record RouteOption(String originCode, String destinationCode, String label) {

    public String key() {
        return originCode + ":" + destinationCode;
    }
}