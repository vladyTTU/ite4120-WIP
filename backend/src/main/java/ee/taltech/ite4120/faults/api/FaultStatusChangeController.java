
package ee.taltech.ite4120.faults.api;

import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.service.FaultStatusChangeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faults/reports/{reportId}/status-changes")
public class FaultStatusChangeController {

    private final FaultStatusChangeService service;

    public FaultStatusChangeController(
            FaultStatusChangeService service) {
        this.service = service;
    }

    // POST: Change the status of a fault report.
    @PostMapping
    public FaultReport changeStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody StatusChangeRequest request) {

        return service.changeStatus(
                reportId, request.toStatus(), request.note());
    }

    // GET: View the report's status-change history.
    @GetMapping
    public List<Map<String, Object>> history(
            @PathVariable Long reportId) {

        return service.history(reportId);
    }

    public record StatusChangeRequest(
            @NotBlank String toStatus,
            String note) {
    }
}
