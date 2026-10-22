
package ee.taltech.ite4120.faults.service;

import ee.taltech.ite4120.faults.addressregistry.AddressInfo;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryAdapter;
import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.repository.FaultReportRepository;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaultReportService {

    private final FaultReportRepository repository;
    private final AddressRegistryAdapter addressRegistry;

    public FaultReportService(
            FaultReportRepository repository,
            AddressRegistryAdapter addressRegistry) {
        this.repository = repository;
        this.addressRegistry = addressRegistry;
    }

    public FaultReport require(Long id) {
        FaultReport report = repository.load(id);

        if (report == null) {
            throw new NotFoundException(
                    "fault report " + id + " not found");
        }

        return report;
    }

    @Transactional
    public FaultReport create(FaultReport report) {
        validateCategory(report);
        validateAddress(report);

        Long id = repository.insert(report);
        return require(id);
    }

    @Transactional
    public FaultReport update(Long id, FaultReport report) {
        FaultReport existing = require(id);

        if (!"NEW".equals(existing.getStatus())) {
            throw new ConflictException(
                    "fault report",
                    "only NEW fault reports can be edited");
        }

        validateCategory(report);
        validateAddress(report);

        report.setId(id);

        if (!repository.update(report)) {
            throw new ConflictException(
                    "fault report",
                    "fault report cannot be edited");
        }

        return require(id);
    }

    @Transactional
    public void retire(Long id) {
        if (!repository.retire(id)) {
            throw new NotFoundException(
                    "fault report " + id + " not found");
        }
    }

    private void validateCategory(FaultReport report) {
        if (!repository.categoryExists(report.getCategoryCode())) {
            throw new ApiClientException(
                    "unknown categoryCode: " + report.getCategoryCode());
        }
    }

    private void validateAddress(FaultReport report) {
        AddressInfo address =
                addressRegistry.lookup(report.getAddressText());

        if (address == null) {
            throw new ApiClientException(
                    "unknown address: " + report.getAddressText());
        }

        report.setAddressText(address.getAddressText());
        report.setAddressCode(address.getAddressCode());
    }
}
