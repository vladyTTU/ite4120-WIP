
package ee.taltech.ite4120.faults.repository;

import ee.taltech.ite4120.faults.model.FaultReport;
import org.helex.commons.db.bean.PgBeanProcessor;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

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

    // List active reports with optional filters, sorting and pagination.
    public List<FaultReport> list(
            String status,
            String categoryCode,
            String addressContains,
            String sort,
            int page,
            int size) {

        StringBuilder sql = new StringBuilder(
                "select * from faults.fault_report " +
                "where sys_status = 'A'");

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" and status = ?");
            params.add(status);
        }

        if (categoryCode != null && !categoryCode.isBlank()) {
            sql.append(" and category_code = ?");
            params.add(categoryCode);
        }

        if (addressContains != null && !addressContains.isBlank()) {
            sql.append(" and address_text ilike ?");
            params.add("%" + addressContains.trim() + "%");
        }

        // Only predefined sorting expressions are allowed.
        String orderBy = switch (sort == null ? "" : sort) {
            case "oldest" -> "id asc";
            case "observedOnAsc" -> "observed_on asc, id asc";
            case "observedOnDesc" -> "observed_on desc, id desc";
            default -> "id desc";
        };

        sql.append(" order by ").append(orderBy);
        sql.append(" limit ? offset ?");

        params.add(size);
        params.add((long) page * size);

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> bp.toBean(rs),
                params.toArray());
    }

    // Count active reports matching the same filters.
    public Long count(
            String status,
            String categoryCode,
            String addressContains) {

        StringBuilder sql = new StringBuilder(
                "select count(*) from faults.fault_report " +
                "where sys_status = 'A'");

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" and status = ?");
            params.add(status);
        }

        if (categoryCode != null && !categoryCode.isBlank()) {
            sql.append(" and category_code = ?");
            params.add(categoryCode);
        }

        if (addressContains != null && !addressContains.isBlank()) {
            sql.append(" and address_text ilike ?");
            params.add("%" + addressContains.trim() + "%");
        }

        return jdbcTemplate.queryForObject(
                sql.toString(),
                Long.class,
                params.toArray());
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

    // Return all available fault category codes.
    public List<String> listCategories() {
        return jdbcTemplate.queryForList(
                "select code from faults.fault_category order by code",
                String.class);
    }
}
