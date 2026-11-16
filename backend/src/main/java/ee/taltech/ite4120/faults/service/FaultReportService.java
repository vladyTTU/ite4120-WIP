
package ee.taltech.ite4120.faults.service;

import ee.taltech.ite4120.faults.addressregistry.AddressInfo;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryAdapter;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryException;
import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.repository.FaultReportRepository;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaultReportService {

    private static final Logger log = LoggerFactory.getLogger(FaultReportService.class);

    private final FaultReportRepository repository;
    private final AddressRegistryAdapter addressRegistry;

    public FaultReportService(
            FaultReportRepository repository,
            AddressRegistryAdapter addressRegistry) {
        this.repository = repository;
        this.addressRegistry = addressRegistry;
    }

    // Find an active fault report by ID.
    public FaultReport require(Long id) {
        FaultReport report = repository.load(id);

        if (report == null) {
            throw new NotFoundException(
                    "fault report " + id + " not found");
        }

        return report;
    }

    // List reports with filtering, sorting and pagination.
    public Map<String, Object> list(
            String status,
            String categoryCode,
            String addressContains,
            String sort,
            int page,
            int size) {

        if (page < 0) {
            throw new ApiClientException(
                    "page must not be negative");
        }

        if (size < 1 || size > 100) {
            throw new ApiClientException(
                    "size must be between 1 and 100");
        }

        List<FaultReport> reports = repository.list(
                status,
                categoryCode,
                addressContains,
                sort,
                page,
                size);

        Long total = repository.count(
                status,
                categoryCode,
                addressContains);

        return Map.of(
                "content", reports,
                "page", page,
                "size", size,
                "totalElements", total,
                "totalPages", (total + size - 1) / size);
    }

    // Summary of active reports grouped by status and category.
    public Map<String, Object> summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byCategory = new LinkedHashMap<>();

        long total = 0;
        int page = 0;
        int size = 100;

        while (true) {
            List<FaultReport> reports = repository.list(
                    null, null, null, "newest", page, size);

            for (FaultReport report : reports) {
                total++;

                byStatus.merge(
                        report.getStatus(), 1L, Long::sum);

                byCategory.merge(
                        report.getCategoryCode(), 1L, Long::sum);
            }

            if (reports.size() < size) {
                break;
            }

            page++;
        }

        return Map.of(
                "total", total,
                "byStatus", byStatus,
                "byCategory", byCategory);
    }

    // Return all available fault category codes.
    public List<String> listCategories() {
        return repository.listCategories();
    }

    // Create a new fault report.
    @Transactional
    public FaultReport create(FaultReport report) {
        validateCategory(report);
        checkAddress(report);

        Long id = repository.insert(report);
        return require(id);
    }

    // Update a report only while its status is NEW.
    @Transactional
    public FaultReport update(Long id, FaultReport report) {
        FaultReport existing = require(id);

        if (!"NEW".equals(existing.getStatus())) {
            throw new ConflictException(
                    "fault report",
                    "a report can only be edited while it is New");
        }

        validateCategory(report);

        // FAULTS.01: the address is checked again only when its text changed.
        String newText = trimmed(report.getAddressText());
        if (newText.equals(existing.getAddressText())) {
            report.setAddressText(existing.getAddressText());
            report.setAddressCode(existing.getAddressCode());
        } else {
            checkAddress(report);
        }

        report.setId(id);

        if (!repository.update(report)) {
            throw new ConflictException(
                    "fault report",
                    "a report can only be edited while it is New");
        }

        return require(id);
    }

    // Soft-delete a fault report.
    @Transactional
    public void retire(Long id) {
        if (!repository.retire(id)) {
            throw new NotFoundException(
                    "fault report " + id + " not found");
        }
    }

    // Validate that the category exists.
    private void validateCategory(FaultReport report) {
        if (!repository.categoryExists(report.getCategoryCode())) {
            throw new ApiClientException(
                    "unknown categoryCode: " + report.getCategoryCode());
        }
    }

    // The address register's answer for a report's address, asked live.
    // Nothing is stored. When the register cannot be reached,
    // AddressRegistryException goes to the caller, which answers 502.
    public AddressInfo liveAddress(Long id) {
        FaultReport report = require(id);
        AddressInfo address = addressRegistry.lookup(report.getAddressText());

        if (address == null) {
            throw new NotFoundException(
                    "address not found in the address register");
        }

        return address;
    }

    // Check the address in the address register (FAULTS.01 § Address register).
    // Found: keep the resident's text and store the register's code.
    // Not found: refuse the report.
    // Register not reachable: fail open, save without a code ("address not verified").
    private void checkAddress(FaultReport report) {
        String text = trimmed(report.getAddressText());
        report.setAddressText(text);

        try {
            AddressInfo address = addressRegistry.lookup(text);

            if (address == null) {
                throw new ApiClientException(
                        "address not found in the address register");
            }

            report.setAddressCode(address.getAddressCode());
        } catch (AddressRegistryException e) {
            log.warn("Address register not reachable; report saved without an address code: {}",
                    e.getMessage());
            report.setAddressCode(null);
        }
    }

    private static String trimmed(String text) {
        return text == null ? "" : text.trim();
    }
}
