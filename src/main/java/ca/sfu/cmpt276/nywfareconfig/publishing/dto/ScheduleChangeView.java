
package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

public record ScheduleChangeView(
    String settingName,
    String baselineValue,
    String stagedValue
) {
    public boolean isChanged() {
        return !baselineValue.equals(stagedValue);
    }
}

    

