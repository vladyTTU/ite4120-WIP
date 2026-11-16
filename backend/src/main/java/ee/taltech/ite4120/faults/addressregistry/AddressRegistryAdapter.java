package ee.taltech.ite4120.faults.addressregistry;

/**
 * The port through which FAULTS asks the address register "is this a real
 * address, and what is its code?" (FAULTS.01 § Address register).
 *
 * <p>The rest of the component depends only on this interface. Which transport
 * answers is the adapter's business: today plain HTTP against the imitated
 * register, later possibly an X-Road service, like the example's
 * {@code OwnerRegistryAdapter}.
 *
 * <p>One method, three outcomes:
 * <ul>
 *   <li>found: the register's address and code;</li>
 *   <li>not found: {@code null};</li>
 *   <li>unreachable (no answer in time, or a fault): {@link AddressRegistryException}.
 *       The caller decides what that means; FAULTS fails open.</li>
 * </ul>
 */
public interface AddressRegistryAdapter {

    /**
     * Look up an address by its text.
     *
     * @param addressText the address as the resident typed it
     * @return the register's address and code, or {@code null} if the register does not know it
     * @throws AddressRegistryException when the register cannot be reached or answers with a fault
     */
    AddressInfo lookup(String addressText);
}
