package ee.taltech.ite4120.faults.addressregistry.mock;

import ee.taltech.ite4120.faults.addressregistry.http.wire.AddressResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The imitated address register — canned addresses served by the application
 * itself, so the FAULTS address check works offline. Enabled only when
 * {@code faults.address-registry.mock-server=true} (the local profile).
 *
 * <p>FAULTS.01 § Address register: {@code GET /mock-registry/addresses?text=...}
 * answers 200 with the address and its code, or 404 when the register does not
 * know the address. The comparison ignores letter case and outer spaces. The codes
 * are made up for the course; they are not real address register codes.
 */
@RestController
@RequestMapping("/mock-registry")
@ConditionalOnProperty(name = "faults.address-registry.mock-server", havingValue = "true")
public class MockAddressRegistryController {

    private static final List<AddressResponse> ADDRESSES = List.of(
            new AddressResponse("Ehitajate tee 5, Tallinn", "ADS-0001"),
            new AddressResponse("Raekoja plats 1, Tartu", "ADS-0002"),
            new AddressResponse("Pikk 12, Pärnu", "ADS-0003"),
            new AddressResponse("Narva mnt 7, Tallinn", "ADS-0004"));

    @GetMapping("/addresses")
    public ResponseEntity<AddressResponse> address(@RequestParam String text) {
        String wanted = normalise(text);
        return ADDRESSES.stream()
                .filter(a -> normalise(a.address()).equals(wanted))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Ignore letter case and outer spaces, as the specification says. */
    private static String normalise(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
