package ca.sfu.cmpt276.nywfareconfig.caracal.exception;

// A specialized subclass of CaracalApiException thrown when 
// the Caracal microservice is offline, unreachable
// or exceeds the 3–5 second timeout.
public class CaracalUnavailableException extends CaracalApiException {
    public CaracalUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
