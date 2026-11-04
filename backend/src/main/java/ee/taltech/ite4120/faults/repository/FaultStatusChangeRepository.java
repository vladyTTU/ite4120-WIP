
package ee.taltech.ite4120.faults.repository;

import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class FaultStatusChangeRepository extends BaseRepository {

    // Update the current status of an active report.
    public boolean updateStatus(Long reportId,
                                String fromStatus,
                                String toStatus) {

        return jdbcTemplate.update("""
                update faults.fault_report
                   set status = ?
                 where id = ?
                   and status = ?
                   and sys_status = 'A'
                """,
                toStatus, reportId, fromStatus) == 1;
    }

    // Save an auditable status-change record.
    public void insert(Long reportId,
                       String fromStatus,
                       String toStatus,
                       String note) {

        jdbcTemplate.update("""
                insert into faults.status_change
                    (report_id, from_status, to_status, note)
                values (?, ?, ?, ?)
                """,
                reportId, fromStatus, toStatus, note);
    }

    // Retrieve active status-change history, oldest first.
    public List<Map<String, Object>> history(Long reportId) {

        return jdbcTemplate.queryForList("""
                select id,
                       report_id as "reportId",
                       from_status as "fromStatus",
                       to_status as "toStatus",
                       note,
                       sys_created_at as "createdAt",
                       sys_created_by as "createdBy"
                  from faults.status_change
                 where report_id = ?
                   and sys_status = 'A'
                 order by id asc
                """,
                reportId);
    }
}
