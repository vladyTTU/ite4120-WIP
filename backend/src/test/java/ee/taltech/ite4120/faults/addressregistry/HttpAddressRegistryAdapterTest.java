package ee.taltech.ite4120.faults.addressregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ee.taltech.ite4120.faults.addressregistry.http.HttpAddressRegistryAdapter;
import ee.taltech.ite4120.faults.addressregistry.http.wire.AddressResponse;
import ee.taltech.ite4120.faults.addressregistry.mock.MockAddressRegistryController;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The address register call over HTTP (FAULTS.01 § Address register): the
 * adapter's three outcomes, found, not found and unreachable.
 *
 * <p>A small web server runs inside the test. Under {@code /register} it answers
 * exactly like the imitated register (it asks {@link MockAddressRegistryController}
 * itself); under {@code /broken} it fails with 500; under {@code /slow} it takes
 * longer than the adapter's 2 seconds.
 */
class HttpAddressRegistryAdapterTest {

    private HttpServer server;
    private ExecutorService threads;
    private String baseUrl;

    @BeforeEach
    void startRegister() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        threads = Executors.newCachedThreadPool(task -> {
            Thread t = new Thread(task, "test-address-register");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(threads);
        MockAddressRegistryController register = new MockAddressRegistryController();

        server.createContext("/register/addresses", exchange -> {
            ResponseEntity<AddressResponse> answer = register.address(textOf(exchange));
            AddressResponse body = answer.getBody();
            if (answer.getStatusCode().value() == 200 && body != null) {
                send(exchange, 200, "{\"address\":\"" + body.address() + "\",\"code\":\"" + body.code() + "\"}");
            } else {
                send(exchange, answer.getStatusCode().value(), "");
            }
        });
        server.createContext("/broken/addresses", exchange -> send(exchange, 500, "{}"));
        server.createContext("/slow/addresses", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            send(exchange, 200, "{\"address\":\"Ehitajate tee 5, Tallinn\",\"code\":\"ADS-0001\"}");
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopRegister() {
        server.stop(0);
        threads.shutdownNow();
    }

    @Test
    void foundTheRegisterAnswersWithTheAddressAndItsCode() {
        AddressInfo info = adapter("/register").lookup("Ehitajate tee 5, Tallinn");

        assertThat(info.getAddressCode()).isEqualTo("ADS-0001");
        assertThat(info.getAddressText()).isEqualTo("Ehitajate tee 5, Tallinn");
    }

    @Test
    void foundLetterCaseOuterSpacesAndEstonianLettersSurviveTheCall() {
        assertThat(adapter("/register").lookup("  ehitajate tee 5, tallinn ").getAddressCode()).isEqualTo("ADS-0001");
        assertThat(adapter("/register").lookup("Pikk 12, Pärnu").getAddressCode()).isEqualTo("ADS-0003");
    }

    @Test
    void notFoundIsAMissNotAFailure() {
        assertThat(adapter("/register").lookup("Nowhere 1, Tallinn")).isNull();
    }

    @Test
    void unreachableWhenTheRegisterAnswersWithAFault() {
        assertThatThrownBy(() -> adapter("/broken").lookup("Ehitajate tee 5, Tallinn"))
                .isInstanceOf(AddressRegistryException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void unreachableWhenTheRegisterDoesNotAnswerWithin2Seconds() {
        long start = System.nanoTime();

        assertThatThrownBy(() -> adapter("/slow").lookup("Ehitajate tee 5, Tallinn"))
                .isInstanceOf(AddressRegistryException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2900));
    }

    @Test
    void unreachableWhenNothingListens() {
        HttpAddressRegistryAdapter nobodyHome = new HttpAddressRegistryAdapter("http://127.0.0.1:9/register");

        assertThatThrownBy(() -> nobodyHome.lookup("Ehitajate tee 5, Tallinn"))
                .isInstanceOf(AddressRegistryException.class)
                .hasMessageContaining("not reachable");
    }

    private HttpAddressRegistryAdapter adapter(String path) {
        return new HttpAddressRegistryAdapter(baseUrl + path);
    }

    private static String textOf(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        String raw = query == null || !query.startsWith("text=") ? "" : query.substring("text=".length());
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } else {
            exchange.close();
        }
    }
}
