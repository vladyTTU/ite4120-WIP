
package ee.taltech.ite4120.faults.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class FaultReportRequest {

    @NotBlank
    private String categoryCode;

    @NotBlank
    @Size(max = 255)
    private String addressText;

    @NotBlank
    @Size(max = 1000)
    private String description;

    private LocalDate observedOn;

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getObservedOn() {
        return observedOn;
    }

    public void setObservedOn(LocalDate observedOn) {
        this.observedOn = observedOn;
    }
}
