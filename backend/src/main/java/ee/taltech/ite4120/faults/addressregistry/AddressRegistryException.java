package ee.taltech.ite4120.faults.addressregistry;

/**
 * The address register could not answer: no answer in time, a transport failure
 * or an upstream fault. On create and edit FAULTS fails open; the live lookup
 * answers 502.
 */
public class AddressRegistryException extends RuntimeException {

    public AddressRegistryException(String message) {
        super(message);
    }

    public AddressRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
