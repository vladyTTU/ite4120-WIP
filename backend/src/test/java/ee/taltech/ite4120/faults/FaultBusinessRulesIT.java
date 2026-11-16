package ee.taltech.ite4120.faults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.taltech.ite4120.faults.addressregistry.AddressInfo;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryAdapter;
import ee.taltech.ite4120.faults.addressregistry.AddressRegistryException;
import ee.taltech.ite4120.faults.model.FaultReport;
import ee.taltech.ite4120.faults.service.FaultReportService;
import ee.taltech.ite4120.faults.service.FaultStatusChangeService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * THE FAULTS BUSINESS TESTS: the executable form of the plain-language tests in
 * docs/src/specifications/FAULTS.01-fault-reporting.md § Business tests. Each
 * test name and number matches the specification.
 *
 * <p>Runs against a real PostgreSQL via Testcontainers, so the whole Liquibase
 * changelog is applied from empty. The address register is replaced by
 * {@link TestRegister}, which knows the same four addresses as the imitated
 * register and can be switched "down". The HTTP call itself is tested in
 * {@code HttpAddressRegistryAdapterTest}.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class FaultBusinessRulesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    FaultReportService reports;

    @Autowired
    FaultStatusChangeService statusChanges;

    @Autowired
    TestRegister register;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        register.down = false;
        SecurityContextHolder.clearContext();
    }

    // ── Creating a report ──────────────────────────────────────────────────

    @Test
    void t01_aValidReportIsAcceptedAndStartsAsNew() {
        signIn("resident1");

        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo("NEW");
        assertThat(created.getAddressCode()).isEqualTo("ADS-0001");
        assertThat(created.getSysCreatedBy()).isEqualTo("resident1");
        assertThat(statusChanges.history(created.getId())).isEmpty();
    }

    @Test
    void t04_theCategoryMustBeOnTheList() {
        assertThatThrownBy(() -> reports.create(report("POTHOLE", "Ehitajate tee 5, Tallinn")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("unknown categoryCode: POTHOLE");
    }

    @Test
    void t06_anAddressTheRegisterDoesNotKnowIsRefused() {
        assertThatThrownBy(() -> reports.create(report("STREETLIGHT", "Nowhere 1, Tallinn")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("address not found in the address register");

        // And nothing is saved.
        assertThat(total(null, null, "Nowhere")).isZero();
    }

    @Test
    void t07_theAddressCheckIgnoresLetterCaseAndOuterSpaces() {
        FaultReport created = reports.create(report("STREETLIGHT", "  ehitajate tee 5, tallinn "));

        assertThat(created.getAddressCode()).isEqualTo("ADS-0001");
        assertThat(created.getAddressText()).isEqualTo("ehitajate tee 5, tallinn");
    }

    @Test
    void t08_anUnreachableAddressRegisterDoesNotBlockAReport() {
        register.down = true;

        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getAddressCode()).as("saved without a code: address not verified").isNull();
    }

    // ── Finding reports ────────────────────────────────────────────────────

    @Test
    void t09_t10_theListShowsActiveReportsNewestFirstAndFiltersCombine() {
        // Narva mnt 7 is used by this test only, so the counts are exact.
        FaultReport streetlightNew = reports.create(report("STREETLIGHT", "Narva mnt 7, Tallinn"));
        FaultReport streetlightResolved = reports.create(report("STREETLIGHT", "Narva mnt 7, Tallinn"));
        statusChanges.changeStatus(streetlightResolved.getId(), "IN_PROGRESS", null);
        statusChanges.changeStatus(streetlightResolved.getId(), "RESOLVED", null);
        FaultReport roadSurfaceNew = reports.create(report("ROAD_SURFACE", "Narva mnt 7, Tallinn"));
        FaultReport retired = reports.create(report("STREETLIGHT", "Narva mnt 7, Tallinn"));
        reports.retire(retired.getId());

        Map<String, Object> filtered = reports.list("NEW", "STREETLIGHT", "Narva", "newest", 0, 20);
        assertThat(ids(filtered)).containsExactly(streetlightNew.getId());
        assertThat(((Number) filtered.get("totalElements")).longValue()).isEqualTo(1);

        // Without filters, newest first; the address search ignores letter case (test 10).
        Map<String, Object> all = reports.list(null, null, "NARVA", "newest", 0, 20);
        assertThat(ids(all)).containsExactly(
                roadSurfaceNew.getId(), streetlightResolved.getId(), streetlightNew.getId());
    }

    @Test
    void t11_theListComesInPagesWithinLimits() {
        assertThatThrownBy(() -> reports.list(null, null, null, "newest", -1, 20))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("page must not be negative");
        assertThatThrownBy(() -> reports.list(null, null, null, "newest", 0, 101))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void t12_theSummaryCountsActiveReportsOnly() {
        long totalBefore = summaryTotal();
        long newBefore = summaryNew();

        reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        FaultReport retired = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        reports.retire(retired.getId());

        assertThat(summaryTotal()).isEqualTo(totalBefore + 1);
        assertThat(summaryNew()).isEqualTo(newBefore + 1);
    }

    // ── Editing and retiring ───────────────────────────────────────────────

    @Test
    void t13_aNewReportCanBeEdited() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        FaultReport edit = report("STREETLIGHT", "Ehitajate tee 5, Tallinn");
        edit.setDescription("Two streetlights are not working");
        FaultReport updated = reports.update(created.getId(), edit);

        assertThat(updated.getDescription()).isEqualTo("Two streetlights are not working");
        assertThat(updated.getStatus()).isEqualTo("NEW");
        assertThat(updated.getSysVersion()).isEqualTo(created.getSysVersion() + 1);
        assertThat(statusChanges.history(created.getId())).isEmpty();
    }

    @Test
    void t14_aReportIsLockedOnceWorkStarts() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        statusChanges.changeStatus(created.getId(), "IN_PROGRESS", null);

        FaultReport edit = report("STREETLIGHT", "Ehitajate tee 5, Tallinn");
        edit.setDescription("Changed after work started");

        assertThatThrownBy(() -> reports.update(created.getId(), edit))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("a report can only be edited while it is New");
        assertThat(reports.require(created.getId()).getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void t15_aChangedAddressIsCheckedAgain() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        FaultReport moved = reports.update(created.getId(), report("STREETLIGHT", "Pikk 12, Pärnu"));
        assertThat(moved.getAddressCode()).isEqualTo("ADS-0003");

        assertThatThrownBy(() -> reports.update(created.getId(), report("STREETLIGHT", "Nowhere 1, Tallinn")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("address not found in the address register");
        FaultReport kept = reports.require(created.getId());
        assertThat(kept.getAddressText()).isEqualTo("Pikk 12, Pärnu");
        assertThat(kept.getAddressCode()).isEqualTo("ADS-0003");
    }

    @Test
    void t15b_anUnchangedAddressIsNotCheckedAgain() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        // With the register down, a new check would lose the code. It is not checked, so the code stays.
        register.down = true;
        FaultReport edit = report("STREETLIGHT", "Ehitajate tee 5, Tallinn");
        edit.setDescription("Still broken");
        FaultReport updated = reports.update(created.getId(), edit);

        assertThat(updated.getAddressCode()).isEqualTo("ADS-0001");
    }

    @Test
    void t16_aRetiredReportDisappearsButIsKept() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        reports.retire(created.getId());

        assertThatThrownBy(() -> reports.require(created.getId())).isInstanceOf(NotFoundException.class);
        Integer kept = jdbc.queryForObject(
                "select count(*) from faults.fault_report where id = ? and sys_status = 'C'",
                Integer.class, created.getId());
        assertThat(kept).isEqualTo(1);
        assertThatThrownBy(() -> reports.retire(created.getId())).isInstanceOf(NotFoundException.class);
    }

    // ── Status and history ─────────────────────────────────────────────────

    @Test
    void t17_aReportMovesForwardAndEveryMoveIsRecorded() {
        signIn("resident1");
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        signIn("officer1");
        assertThat(statusChanges.changeStatus(created.getId(), "IN_PROGRESS", null).getStatus())
                .isEqualTo("IN_PROGRESS");
        assertThat(statusChanges.changeStatus(created.getId(), "RESOLVED", "Lamp replaced").getStatus())
                .isEqualTo("RESOLVED");

        List<Map<String, Object>> history = statusChanges.history(created.getId());
        assertThat(history)
                .extracting(row -> row.get("fromStatus") + " -> " + row.get("toStatus") + " by " + row.get("createdBy"))
                .containsExactly("NEW -> IN_PROGRESS by officer1", "IN_PROGRESS -> RESOLVED by officer1");
    }

    @Test
    void t18_aStatusNeverSkipsNeverGoesBackAndFinalStatusesAreFinal() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();

        assertThatThrownBy(() -> statusChanges.changeStatus(id, "RESOLVED", null))
                .as("work must start first")
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("status transition is not allowed");

        statusChanges.changeStatus(id, "IN_PROGRESS", null);
        statusChanges.changeStatus(id, "RESOLVED", null);

        assertThatThrownBy(() -> statusChanges.changeStatus(id, "IN_PROGRESS", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("status transition is not allowed");
        assertThatThrownBy(() -> statusChanges.changeStatus(id, "RESOLVED", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("report already has this status");
        assertThat(reports.require(id).getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void t19_aRejectionNeedsAReason() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();

        assertThatThrownBy(() -> statusChanges.changeStatus(id, "REJECTED", null))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("rejection requires an explanation");
        assertThatThrownBy(() -> statusChanges.changeStatus(id, "REJECTED", "   "))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("rejection requires an explanation");

        FaultReport rejected = statusChanges.changeStatus(id, "REJECTED", "Private property, not municipal");

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        assertThat(statusChanges.history(id))
                .extracting(row -> row.get("note"))
                .containsExactly("Private property, not municipal");
    }

    @Test
    void t20_anUnknownStatusIsRefused() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        assertThatThrownBy(() -> statusChanges.changeStatus(created.getId(), "OPEN", null))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("invalid status");
    }

    @Test
    void t21_aMistakenChangeCanBeUndoneAndTheUndoIsKept() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();
        statusChanges.changeStatus(id, "IN_PROGRESS", null);
        statusChanges.changeStatus(id, "RESOLVED", null);

        assertThatThrownBy(() -> statusChanges.undoLastChange(id, "  "))
                .as("an undo needs a reason")
                .isInstanceOf(ApiClientException.class);

        FaultReport undone = statusChanges.undoLastChange(id, "Marked resolved by mistake");

        assertThat(undone.getStatus()).isEqualTo("IN_PROGRESS");
        List<Map<String, Object>> history = statusChanges.history(id);
        assertThat(history).hasSize(3);
        Map<String, Object> undo = history.get(2);
        assertThat(undo.get("fromStatus")).isEqualTo("RESOLVED");
        assertThat(undo.get("toStatus")).isEqualTo("IN_PROGRESS");
        assertThat(undo.get("note")).isEqualTo("UNDO: Marked resolved by mistake");
    }

    @Test
    void t22_onlyTheLastOrdinaryChangeCanBeUndone() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();

        assertThatThrownBy(() -> statusChanges.undoLastChange(id, "Nothing happened yet"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no status change available to undo");

        statusChanges.changeStatus(id, "IN_PROGRESS", null);
        statusChanges.undoLastChange(id, "Started by mistake");

        assertThatThrownBy(() -> statusChanges.undoLastChange(id, "Once more"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("latest status change is already an undo");
    }

    @Test
    void t23_undoingTheStartOfWorkMakesTheReportEditableAgain() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();
        statusChanges.changeStatus(id, "IN_PROGRESS", null);

        assertThat(statusChanges.undoLastChange(id, "Started by mistake").getStatus()).isEqualTo("NEW");

        FaultReport edit = report("STREETLIGHT", "Ehitajate tee 5, Tallinn");
        edit.setDescription("Edited after the undo");
        assertThat(reports.update(id, edit).getDescription()).isEqualTo("Edited after the undo");
    }

    @Test
    void t24_aRetiredReportCannotBeChanged() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));
        Long id = created.getId();
        reports.retire(id);

        assertThatThrownBy(() -> reports.update(id, report("STREETLIGHT", "Ehitajate tee 5, Tallinn")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> statusChanges.changeStatus(id, "IN_PROGRESS", null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> statusChanges.undoLastChange(id, "Too late"))
                .isInstanceOf(NotFoundException.class);
    }

    // ── Address register, live ─────────────────────────────────────────────

    @Test
    void t26_theRegistersAnswerIsShownLiveAndTheReportNeverDependsOnIt() {
        FaultReport created = reports.create(report("STREETLIGHT", "Ehitajate tee 5, Tallinn"));

        AddressInfo live = reports.liveAddress(created.getId());
        assertThat(live.getAddressText()).isEqualTo("Ehitajate tee 5, Tallinn");
        assertThat(live.getAddressCode()).isEqualTo("ADS-0001");

        register.down = true;
        assertThatThrownBy(() -> reports.liveAddress(created.getId()))
                .isInstanceOf(AddressRegistryException.class);
        assertThat(reports.require(created.getId()).getStatus()).isEqualTo("NEW");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static final String DESCRIPTION = "Streetlight is not working";

    private static FaultReport report(String categoryCode, String addressText) {
        FaultReport r = new FaultReport();
        r.setCategoryCode(categoryCode);
        r.setAddressText(addressText);
        r.setDescription(DESCRIPTION);
        r.setObservedOn(LocalDate.now().minusDays(1));
        return r;
    }

    @SuppressWarnings("unchecked")
    private static List<Long> ids(Map<String, Object> page) {
        return ((List<FaultReport>) page.get("content")).stream().map(FaultReport::getId).toList();
    }

    private long total(String status, String categoryCode, String addressContains) {
        return ((Number) reports.list(status, categoryCode, addressContains, "newest", 0, 20)
                .get("totalElements")).longValue();
    }

    private long summaryTotal() {
        return ((Number) reports.summary().get("total")).longValue();
    }

    @SuppressWarnings("unchecked")
    private long summaryNew() {
        Map<String, Long> byStatus = (Map<String, Long>) reports.summary().get("byStatus");
        return byStatus.getOrDefault("NEW", 0L);
    }

    private static void signIn(String user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Stands in for the address register: the same four addresses as the
     * imitated register (FAULTS.01 § Address register), compared ignoring letter
     * case and outer spaces. Switch {@code down} on to simulate an unreachable
     * register.
     */
    static class TestRegister implements AddressRegistryAdapter {

        volatile boolean down;

        private static final List<AddressInfo> KNOWN = List.of(
                new AddressInfo("ADS-0001", "Ehitajate tee 5, Tallinn"),
                new AddressInfo("ADS-0002", "Raekoja plats 1, Tartu"),
                new AddressInfo("ADS-0003", "Pikk 12, Pärnu"),
                new AddressInfo("ADS-0004", "Narva mnt 7, Tallinn"));

        @Override
        public AddressInfo lookup(String addressText) {
            if (down) {
                throw new AddressRegistryException("address register not reachable (test)");
            }
            String wanted = addressText == null ? "" : addressText.trim().toLowerCase(Locale.ROOT);
            return KNOWN.stream()
                    .filter(a -> a.getAddressText().toLowerCase(Locale.ROOT).equals(wanted))
                    .findFirst()
                    .orElse(null);
        }
    }

    @TestConfiguration
    static class TestRegisterConfig {

        @Bean
        @Primary
        TestRegister testRegister() {
            return new TestRegister();
        }
    }
}
