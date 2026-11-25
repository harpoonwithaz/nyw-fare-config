package ca.sfu.cmpt276.nywfareconfig.alert.dto;

public record ProductOption(String code, String name) {

    public String label() {
        return name == null || name.isBlank() ? code : name;
    }
}