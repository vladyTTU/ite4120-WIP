
package ee.taltech.ite4120.faults.api;

import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.service.FaultReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faults")
public class FaultReportController {

    private final FaultReportService service;

    public FaultReportController(FaultReportService service) {
        this.service = service;
    }

    // GET: Find a fault report by ID.
    @GetMapping("/{id}")
    public FaultReport get(@PathVariable Long id) {
        return service.require(id);
    }

    // POST: Create a new fault report.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaultReport create(
            @Valid @RequestBody FaultReportRequest request) {

        return service.create(toReport(request));
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
