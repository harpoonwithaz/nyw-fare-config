package ca.sfu.cmpt276.nywfareconfig.caracal.exception;

// The base unchecked runtime exception thrown when Caracal returns 
// unexpected 4xx errors (such as an invalid tenantCode or malformed payload) 
// or 5xx server failures, allowing controllers to catch errors 
// cleanly and render Thymeleaf error banners.
public class CaracalApiException extends RuntimeException {
    
    public CaracalApiException() {
        super();
    }

    public CaracalApiException(String message) {
        super(message);
    }

    public CaracalApiException(Throwable cause) {
        super(cause);
    }

    public CaracalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
