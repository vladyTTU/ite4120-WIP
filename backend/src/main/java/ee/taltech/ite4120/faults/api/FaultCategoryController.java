
package ee.taltech.ite4120.faults.api;

import ee.taltech.ite4120.faults.service.FaultReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faults/categories")
public class FaultCategoryController {

    private final FaultReportService service;

    public FaultCategoryController(FaultReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> list() {
        return service.listCategories();
    }
}
