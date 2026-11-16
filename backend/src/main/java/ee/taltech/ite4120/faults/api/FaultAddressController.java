package ee.taltech.ite4120.faults.api;

import ee.taltech.ite4120.faults.addressregistry.AddressInfo;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryException;
import ee.taltech.ite4120.faults.service.FaultReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The address register's answer for a report, asked live (FAULTS.01 § Address
 * register), in the same way as the example's {@code /animals/{id}/owner}. The
 * answer is shown on the screen and never stored.
 */
@RestController
@RequestMapping("/api/faults/reports/{id}/address")
public class FaultAddressController {

    private final FaultReportService service;

    public FaultAddressController(FaultReportService service) {
        this.service = service;
    }

    @Operation(summary = "The address register's answer for a report's address, asked live",
            description = "Calls the address register through the AddressRegistryAdapter. "
                    + "Nothing is stored; the report itself never depends on this answer.")
    @ApiResponse(responseCode = "200", description = "the register knows the address")
    @ApiResponse(responseCode = "404", description = "no active report, or the register does not know the address")
    @ApiResponse(responseCode = "502", description = "address register not reachable")
    @GetMapping
    public AddressAnswer address(@PathVariable Long id) {
        AddressInfo info = service.liveAddress(id);
        return new AddressAnswer(info.getAddressText(), info.getAddressCode());
    }

    /** The register being down is an upstream failure, not the caller's fault: 502. */
    @ExceptionHandler(AddressRegistryException.class)
    public ProblemDetail registerDown(AddressRegistryException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Address register unavailable");
        pd.setDetail("address register not reachable");
        return pd;
    }

    /** The live answer, in the register's own words: {@code { "address", "code" }}. */
    public record AddressAnswer(String address, String code) {
    }
}
