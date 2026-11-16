
package ee.taltech.ite4120.faults.addressregistry;

/**
 * Interface for validating addresses through an external registry.
 *
 * The implementation can be a simulated registry now
 * and an X-Road service in the future.
 */
public interface AddressRegistryAdapter {

    /**
     * Look up an address using its text.
     *
     * @param addressText address entered by the user
     * @return validated address information, or null if not found
     */
    AddressInfo lookup(String addressText);
}
