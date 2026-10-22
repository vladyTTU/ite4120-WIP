package ee.taltech.ite4120.faults.addressregistry.http.wire;

/**
 * WIRE RECORD — the address register's answer exactly as the register sends it.
 *
 * <p>FAULTS.01 § Address register: until the real service's schema is known, the
 * imitation answers with {@code address} and {@code code}. When the real schema is
 * known, its field names are copied in here unchanged, and the adapter's mapper is
 * the only place that translates them into our own names.
 */
public record AddressResponse(
        String address,
        String code) {}
