
package ee.taltech.ite4120.faults.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.service.FaultStatusChangeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Faults", description = "FAULTS: public infrastructure fault reporting (FAULTS.01)")
@RequestMapping("/api/faults/reports/{reportId}/status-changes")
public class FaultStatusChangeController {

    private final FaultStatusChangeService service;

    public FaultStatusChangeController(
            FaultStatusChangeService service) {
        this.service = service;
    }

    // POST: Change a report's status.
    @Operation(summary = "Move a report to its next status; a rejection needs a reason")
    @ApiResponse(responseCode = "200", description = "The updated report")
    @ApiResponse(responseCode = "400", description = "Unknown status, or a rejection without a reason")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @ApiResponse(responseCode = "409", description = "The move is not in the status table")
    @PostMapping
    public FaultReport changeStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody StatusChangeRequest request) {

        return service.changeStatus(
                reportId, request.toStatus(), request.note());
    }

    // GET: View status-change history.
    @Operation(summary = "The report's status history, oldest first")
    @ApiResponse(responseCode = "200", description = "The history rows")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @GetMapping
    public List<Map<String, Object>> history(
            @PathVariable Long reportId) {

        return service.history(reportId);
    }

    // POST: Undo latest status change with a required reason.
    @Operation(summary = "Undo the last status change, with a reason; the undo is kept in the history")
    @ApiResponse(responseCode = "200", description = "The updated report")
    @ApiResponse(responseCode = "400", description = "No reason given")
    @ApiResponse(responseCode = "404", description = "No active report with that id")
    @ApiResponse(responseCode = "409", description = "Nothing to undo, or the last change is already an undo")
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
