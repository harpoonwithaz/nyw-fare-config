package ca.sfu.cmpt276.nywfareconfig.security.model;

public enum Role {
    OPERATOR("Operator"),
    CLIENT_SERVICES("Client Services (Admin)");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}