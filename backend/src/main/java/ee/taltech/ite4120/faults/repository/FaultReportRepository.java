
package ee.taltech.ite4120.faults.repository;

import ee.taltech.ite4120.faults.model.FaultReport;
import org.helex.commons.db.bean.PgBeanProcessor;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public class FaultReportRepository extends BaseRepository {

    private final PgBeanProcessor bp =
            new PgBeanProcessor(FaultReport.class);

    // Find one active fault report by its ID.
    public FaultReport load(Long id) {
        return getBean(
                "select * from faults.fault_report " +
                "where id = ? and sys_status = 'A'",
                bp, id);
    }

    // Save a new fault report and return its ID.
    public Long insert(FaultReport report) {
        String sql = """
                insert into faults.fault_report
                    (category_code, address_text, address_code,
                     description, observed_on)
                values (?, ?, ?, ?, ?)
                returning id
                """;

        return queryForObject(sql, Long.class,
                report.getCategoryCode(),
                report.getAddressText(),
                report.getAddressCode(),
                report.getDescription(),
                report.getObservedOn());
    }

    // Update an active report only while its status is NEW.
    public boolean update(FaultReport report) {
        String sql = """
                update faults.fault_report
                   set category_code = ?,
                       address_text = ?,
                       address_code = ?,
                       description = ?,
                       observed_on = ?
                 where id = ?
                   and sys_status = 'A'
                   and status = 'NEW'
                """;

        return jdbcTemplate.update(sql,
                report.getCategoryCode(),
                report.getAddressText(),
                report.getAddressCode(),
                report.getDescription(),
                report.getObservedOn(),
                report.getId()) == 1;
    }

    // Soft-delete: keep the record but mark it inactive.
    public boolean retire(Long id) {
        return jdbcTemplate.update(
                "update faults.fault_report " +
                "set sys_status = 'C' " +
                "where id = ? and sys_status = 'A'",
                id) == 1;
    }

    // Check whether the fault category exists.
    public boolean categoryExists(String code) {
        Integer n = queryForObject(
                "select count(1) from faults.fault_category where code = ?",
                Integer.class, code);
        return n != null && n > 0;
    }
}
