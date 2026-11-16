
package ee.taltech.ite4120.faults.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class FaultReport {

    private Long id;
    private String categoryCode;
    private String addressText;
    private String addressCode;
    private String description;
    private LocalDate observedOn;
    private String status;

    private String sysStatus;
    private Integer sysVersion;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;
    private OffsetDateTime sysModifiedAt;
    private String sysModifiedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }

    public String getAddressCode() { return addressCode; }
    public void setAddressCode(String addressCode) { this.addressCode = addressCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getObservedOn() { return observedOn; }
    public void setObservedOn(LocalDate observedOn) { this.observedOn = observedOn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSysStatus() { return sysStatus; }
    public void setSysStatus(String sysStatus) { this.sysStatus = sysStatus; }

    public Integer getSysVersion() { return sysVersion; }
    public void setSysVersion(Integer sysVersion) { this.sysVersion = sysVersion; }

    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }

    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }

    public OffsetDateTime getSysModifiedAt() { return sysModifiedAt; }
    public void setSysModifiedAt(OffsetDateTime sysModifiedAt) { this.sysModifiedAt = sysModifiedAt; }

    public String getSysModifiedBy() { return sysModifiedBy; }
    public void setSysModifiedBy(String sysModifiedBy) { this.sysModifiedBy = sysModifiedBy; }
}
