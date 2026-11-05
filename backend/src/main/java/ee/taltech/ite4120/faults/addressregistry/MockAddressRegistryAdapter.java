
package ee.taltech.ite4120.faults.addressregistry;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Simulated address registry for local development.
 * No real X-Road connection is required.
 */
@Component
public class MockAddressRegistryAdapter implements AddressRegistryAdapter {

    private static final Map<String, AddressInfo> ADDRESSES = Map.of(
            "Ehitajate tee 5, Tallinn",
            new AddressInfo("ADDR-001", "Ehitajate tee 5, Tallinn"),

            "Raekoja plats 1, Tartu",
            new AddressInfo("ADDR-002", "Raekoja plats 1, Tartu"),

            "Pikk 12, Pärnu",
            new AddressInfo("ADDR-003", "Pikk 12, Pärnu")
    );

    @Override
    public AddressInfo lookup(String addressText) {
        if (addressText == null) {
            return null;
        }

        String search = addressText.trim();

        for (Map.Entry<String, AddressInfo> entry : ADDRESSES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(search)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
