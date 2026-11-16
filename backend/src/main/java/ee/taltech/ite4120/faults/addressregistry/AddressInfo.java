
package ee.taltech.ite4120.faults.addressregistry;

/**
 * Validated address information returned by the address registry.
 */
public class AddressInfo {

    private final String addressCode;
    private final String addressText;

    public AddressInfo(String addressCode, String addressText) {
        this.addressCode = addressCode;
        this.addressText = addressText;
    }

    public String getAddressCode() {
        return addressCode;
    }

    public String getAddressText() {
        return addressText;
    }
}
