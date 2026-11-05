
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

    // POST: Change a report's status.
    @PostMapping
    public FaultReport changeStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody StatusChangeRequest request) {

        return service.changeStatus(
                reportId, request.toStatus(), request.note());
    }

    // GET: View status-change history.
    @GetMapping
    public List<Map<String, Object>> history(
            @PathVariable Long reportId) {

        return service.history(reportId);
    }

    // POST: Undo latest status change with a required reason.
    @PostMapping("/undo")
    public FaultReport undoLastChange(
            @PathVariable Long reportId,
            @Valid @RequestBody UndoRequest request) {

        return service.undoLastChange(reportId, request.note());
    }

    public record StatusChangeRequest(
            @NotBlank String toStatus,
            String note) {
    }

    public record UndoRequest(
            @NotBlank String note) {
    }
}
