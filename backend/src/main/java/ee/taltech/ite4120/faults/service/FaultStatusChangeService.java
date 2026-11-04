
package ee.taltech.ite4120.faults.service;

import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.repository.FaultStatusChangeRepository;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FaultStatusChangeService {

    private static final Set<String> VALID_STATUSES =
            Set.of("NEW", "IN_PROGRESS", "RESOLVED", "REJECTED");

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

        // Basic workflow: NEW -> IN_PROGRESS -> final status.
        boolean allowed =
                ("NEW".equals(fromStatus)
                        && "IN_PROGRESS".equals(toStatus))
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

    public List<Map<String, Object>> history(Long reportId) {
        reportService.require(reportId);
        return repository.history(reportId);
    }
}
