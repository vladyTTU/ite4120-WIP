
package ee.taltech.ite4120.faults.api;

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
@RequestMapping("/api/faults/reports")
public class FaultReportController {

    private final FaultReportService service;

    public FaultReportController(FaultReportService service) {
        this.service = service;
    }

    // GET: List reports with filters, sorting and pagination.
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
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return service.summary();
    }

    // GET: Find a fault report by ID.
    @GetMapping("/{id}")
    public FaultReport get(@PathVariable Long id) {
        return service.require(id);
    }

    // POST: Create a report and return its Location.
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
    @PutMapping("/{id}")
    public FaultReport update(
            @PathVariable Long id,
            @Valid @RequestBody FaultReportRequest request) {

        return service.update(id, toReport(request));
    }

    // DELETE: Soft-retire a fault report.
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
