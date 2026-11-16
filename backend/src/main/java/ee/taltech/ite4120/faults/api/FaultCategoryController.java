
package ee.taltech.ite4120.faults.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ee.taltech.ite4120.faults.service.FaultReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Faults", description = "FAULTS: public infrastructure fault reporting (FAULTS.01)")
@RequestMapping("/api/faults/categories")
public class FaultCategoryController {

    private final FaultReportService service;

    public FaultCategoryController(FaultReportService service) {
        this.service = service;
    }

    @Operation(summary = "Fault category reference list, as codes")
    @GetMapping
    public List<String> list() {
        return service.listCategories();
    }
}
