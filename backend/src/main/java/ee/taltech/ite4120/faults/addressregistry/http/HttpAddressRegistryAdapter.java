package ee.taltech.ite4120.faults.addressregistry.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ite4120.faults.addressregistry.AddressInfo;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryAdapter;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryException;
import ee.taltech.ite4120.faults.addressregistry.http.wire.AddressResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calls the address register over plain HTTP + JSON and translates its answer
 * into our {@link AddressInfo} (FAULTS.01 § Address register).
 *
 * <p>Locally the URL in {@code faults.address-registry.url} points back at this
 * application's own {@code /mock-registry/addresses}: the imitated register. A
 * real register needs only a different URL. To simulate a register that is down,
 * point the URL at a port where nothing listens.
 *
 * <p>The register gets 2 seconds to answer. A 404 means "not found"; no answer in
 * time, a refused connection or any other status is a failure
 * ({@link AddressRegistryException}).
 */
@Component
public class HttpAddressRegistryAdapter implements AddressRegistryAdapter {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public HttpAddressRegistryAdapter(
            @Value("${faults.address-registry.url:http://localhost:18440/mock-registry}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public AddressInfo lookup(String addressText) {
        String text = addressText == null ? "" : addressText;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/addresses?text="
                        + URLEncoder.encode(text, StandardCharsets.UTF_8)))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                // An address the register does not know is a miss, not a failure.
                return null;
            }
            if (response.statusCode() != 200) {
                throw new AddressRegistryException(
                        "address register answered HTTP " + response.statusCode());
            }
            AddressResponse wire = json.readValue(response.body(), AddressResponse.class);
            // The mapper: the only place where the register's names become ours.
            return new AddressInfo(wire.code(), wire.address());
        } catch (IOException e) {
            // Includes no answer within 2 seconds and a refused connection.
            throw new AddressRegistryException("address register not reachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AddressRegistryException("address register call interrupted", e);
        }
    }
}
