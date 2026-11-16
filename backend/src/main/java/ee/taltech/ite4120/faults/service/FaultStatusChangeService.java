
package ee.taltech.ite4120.faults.service;

import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.repository.FaultStatusChangeRepository;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FaultStatusChangeService {

    private static final Set<String> VALID_STATUSES =
            Set.of("NEW", "IN_PROGRESS", "RESOLVED", "REJECTED");

    private static final String UNDO_PREFIX = "UNDO: ";

    private final FaultReportService reportService;
    private final FaultStatusChangeRepository repository;

    public FaultStatusChangeService(
            FaultReportService reportService,
            FaultStatusChangeRepository repository) {
        this.reportService = reportService;
        this.repository = repository;
    }

    @Transactional
    public FaultReport changeStatus(
            Long reportId, String toStatus, String note) {

        FaultReport report = reportService.require(reportId);

        if (toStatus == null || !VALID_STATUSES.contains(toStatus)) {
            throw new ApiClientException("invalid status");
        }

        if (note != null && note.length() > 500) {
            throw new ApiClientException(
                    "note must not exceed 500 characters");
        }

        if ("REJECTED".equals(toStatus)
                && (note == null || note.isBlank())) {
            throw new ApiClientException(
                    "rejection requires an explanation");
        }

        String fromStatus = report.getStatus();

        if (fromStatus.equals(toStatus)) {
            throw new ConflictException(
                    "fault report",
                    "report already has this status");
        }

        // Normal workflow: NEW -> IN_PROGRESS -> final.
        // NEW -> REJECTED is also allowed.
        boolean allowed =
                ("NEW".equals(fromStatus)
                        && ("IN_PROGRESS".equals(toStatus)
                        || "REJECTED".equals(toStatus)))
                || ("IN_PROGRESS".equals(fromStatus)
                        && ("RESOLVED".equals(toStatus)
                        || "REJECTED".equals(toStatus)));

        if (!allowed) {
            throw new ConflictException(
                    "fault report",
                    "status transition is not allowed");
        }

        if (!repository.updateStatus(
                reportId, fromStatus, toStatus)) {
            throw new ConflictException(
                    "fault report",
                    "status was changed by another request");
        }

        repository.insert(
                reportId, fromStatus, toStatus, note);

        return reportService.require(reportId);
    }

    // Undo the latest normal status change once.
    @Transactional
    public FaultReport undoLastChange(Long reportId, String reason) {

        FaultReport report = reportService.require(reportId);

        if (reason == null || reason.isBlank()) {
            throw new ApiClientException(
                    "undo requires an explanation");
        }

        String cleanReason = reason.trim();

        if (UNDO_PREFIX.length() + cleanReason.length() > 500) {
            throw new ApiClientException(
                    "undo explanation is too long");
        }

        Map<String, Object> latest =
                repository.latestChange(reportId);

        if (latest == null) {
            throw new ConflictException(
                    "fault report",
                    "no status change available to undo");
        }

        String previousStatus =
                (String) latest.get("fromStatus");

        String changedStatus =
                (String) latest.get("toStatus");

        String latestNote =
                (String) latest.get("note");

        // Prevent undoing an undo.
        if (latestNote != null
                && latestNote.startsWith(UNDO_PREFIX)) {
            throw new ConflictException(
                    "fault report",
                    "latest status change is already an undo");
        }

        if (previousStatus == null) {
            throw new ConflictException(
                    "fault report",
                    "previous status is unavailable");
        }

        if (!report.getStatus().equals(changedStatus)) {
            throw new ConflictException(
                    "fault report",
                    "report status no longer matches history");
        }

        if (!repository.updateStatus(
                reportId, changedStatus, previousStatus)) {
            throw new ConflictException(
                    "fault report",
                    "status was changed by another request");
        }

        // Preserve history and save the officer's explanation.
        repository.insert(
                reportId,
                changedStatus,
                previousStatus,
                UNDO_PREFIX + cleanReason);

        return reportService.require(reportId);
    }

    public List<Map<String, Object>> history(Long reportId) {
        reportService.require(reportId);
        return repository.history(reportId);
    }
}
