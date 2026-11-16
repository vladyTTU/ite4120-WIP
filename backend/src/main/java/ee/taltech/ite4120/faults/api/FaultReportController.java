
package ee.taltech.ite4120.faults.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.service.FaultReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@Tag(name = "Faults", description = "FAULTS: public infrastructure fault reporting (FAULTS.01)")
@RequestMapping("/api/faults/reports")
public class FaultReportController {

    private final FaultReportService service;

    public FaultReportController(FaultReportService service) {
        this.service = service;
    }

    // GET: List reports with filters, sorting and pagination.
    @Operation(summary = "Search reports: filter by status, category and address; paged; newest first")
    @ApiResponse(responseCode = "200", description = "One page of reports")
    @ApiResponse(responseCode = "400", description = "page or size out of range")
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String addressContains,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return service.list(
                status, categoryCode, addressContains,
                sort, page, size);
    }

    // GET: Summary of active reports by status and category.
    @Operation(summary = "Count active reports: in total, by status and by category")
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return service.summary();
    }

    // GET: Find a fault report by ID.
    @Operation(summary = "One active report by id")
    @ApiResponse(responseCode = "200", description = "The report")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @GetMapping("/{id}")
    public FaultReport get(@PathVariable Long id) {
        return service.require(id);
    }

    // POST: Create a report and return its Location.
    @Operation(summary = "Report a fault; the address is checked in the address register")
    @ApiResponse(responseCode = "201", description = "Created; the Location header points at the new report")
    @ApiResponse(responseCode = "400", description = "Invalid request, unknown category, or an address the register does not know")
    @PostMapping
    public ResponseEntity<FaultReport> create(
            @Valid @RequestBody FaultReportRequest request) {

        FaultReport created = service.create(toReport(request));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // PUT: Update an existing NEW fault report.
    @Operation(summary = "Edit a report, only while it is New; a changed address is checked again")
    @ApiResponse(responseCode = "200", description = "The updated report")
    @ApiResponse(responseCode = "400", description = "Invalid request, unknown category, or an address the register does not know")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @ApiResponse(responseCode = "409", description = "The report is no longer New")
    @PutMapping("/{id}")
    public FaultReport update(
            @PathVariable Long id,
            @Valid @RequestBody FaultReportRequest request) {

        return service.update(id, toReport(request));
    }

    // DELETE: Soft-retire a fault report.
    @Operation(summary = "Retire a report (soft delete): hidden from all lists, kept in the database")
    @ApiResponse(responseCode = "204", description = "Retired")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retire(@PathVariable Long id) {
        service.retire(id);
    }

    private FaultReport toReport(FaultReportRequest request) {
        FaultReport report = new FaultReport();

        report.setCategoryCode(request.getCategoryCode());
        report.setAddressText(request.getAddressText());
        report.setDescription(request.getDescription());
        report.setObservedOn(request.getObservedOn());

        return report;
    }
}
