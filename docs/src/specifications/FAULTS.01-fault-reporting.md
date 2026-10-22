# FAULTS.01 · Fault reporting

> **FAULTS.01** · state **Agreed** · traces from [FAULTS-US-001](../user-stories/FAULTS-US-001.md)

## Description

A register of fault reports for public infrastructure: a resident reports a
fault, a municipal officer tracks it to a decision. Create, search, view, edit
and retire a report; change its status; undo a mistaken status change; show
counts by status and category. The location is checked against an address
register through an adapter, as the worked example does for owners. In this
course the register is imitated inside the application.

Excluded: photos and attachments, maps and coordinates, e-mail or SMS notices,
routing a report to a specific authority, roles (any signed-in user can do
everything), real authentication (mock auth only), any personal data beyond the
platform `sys_*` columns.

## Data model  *(→ session 2)*

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `faults.fault_category` | `code` PK | text | reference list: STREETLIGHT, ROAD_SURFACE, TRAFFIC_SIGN, PAVEMENT, PLAYGROUND, OTHER |
| | `name` | text | display name, required |
| `faults.fault_report` | `id` PK | bigint | from `core.seq_id`; shown to the resident as the report number |
| | `category_code` | text | required; FK → `fault_category` |
| | `address_text` | text | required, ≤255; the address as the resident typed it, without outer spaces |
| | `address_code` | text | optional; the code the address register returned; empty means "not verified" |
| | `description` | text | required, not blank, ≤1000 |
| | `observed_on` | date | optional; **not in the future** |
| | `status` | text | required; one of NEW, IN_PROGRESS, RESOLVED, REJECTED; starts as NEW |
| | `sys_*` | — | platform columns, trigger-managed; `sys_created_by` = the reporter |
| `faults.status_change` | `id` PK | bigint | from `core.seq_id` |
| | `report_id` | bigint | required; FK → `fault_report` |
| | `from_status` | text | empty for the first entry |
| | `to_status` | text | required; same four values as `status` |
| | `note` | text | optional, ≤500; **required when `to_status` is REJECTED** (checked by the database too) **and for a correction** (checked by the service) |
| | `sys_*` | — | platform columns; `sys_created_at` / `sys_created_by` = when and who |

Three tables in schema `faults`, two foreign keys. `fault_category` is a
reference table like `species`: natural key, no `sys_*` columns, its own seed
changeset. Reports are never deleted: retiring sets `sys_status` from A to C,
and every read filters on A.

### Status rules

| Current status | Allowed next status |
| --- | --- |
| NEW | IN_PROGRESS, REJECTED |
| IN_PROGRESS | RESOLVED, REJECTED |
| RESOLVED | none (final) |
| REJECTED | none (final) |

Every move adds one `status_change` row. A **correction** undoes the last
ordinary move: the report goes back to the status it came from, and a new row
records the move back with its reason in `note`. A correction is recognised as
a row whose pair (`from_status`, `to_status`) is not in the table above. Only
the last row can be corrected, and only if it is an ordinary move: the first
row (empty → NEW) and a correction cannot be corrected.

Changesets (done, on branch `faults-liquibase-api`): `faults-schema.sql`,
`01-fault-category.sql`, `02-fault-report.sql`, `03-status-change.sql`,
`90-demo-data.sql` (`context:demo`). This update needs no database change.

## API  *(→ session 3)*

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/faults/categories` | the reference list | — |
| `GET /api/faults/reports` | paged search: `status`, `categoryCode`, `addressContains`, `limit`, `offset`, `sort`; newest first by default | 400 unknown sort key or filter value |
| `GET /api/faults/reports/{id}` | one active report | 404 |
| `POST /api/faults/reports` | create a report; checks the address; adds the first history row; answers **201 + Location** | 400 |
| `PUT /api/faults/reports/{id}` | edit category, address, description and date seen; **only while NEW**; the address is checked again if it changed; answers 200 with the report | 400 · 404 · 409 not New |
| `DELETE /api/faults/reports/{id}` | retire (soft delete), at any status; answers **204** | 404 |
| `GET /api/faults/reports/{id}/status-changes` | the status history, oldest first | 404 |
| `POST /api/faults/reports/{id}/status-changes` | move the report to a new status; answers **201** with the new history row | 400 · 404 · 409 move not allowed |
| `POST /api/faults/reports/{id}/corrections` | undo the last status change, with a required note; answers **201** with the new history row | 400 · 404 · 409 nothing to correct |
| `GET /api/faults/reports/summary` | count of active reports per category and status | — |

Paths name things (reports, status changes, corrections), never actions; the
HTTP method says what happens. All endpoints require
`Authorization: Bearer <user>` (mock auth); without it, 401. Every error is
`application/problem+json`. Every endpoint is documented in Swagger
(`@Operation`, `@ApiResponse` for each failure answer), live at `/swagger-ui.html`.

**Request bodies.**
Create and edit: `{ "categoryCode", "addressText", "description", "observedOn" }`.
Status and `sys*` fields are never accepted on input; they are ignored.
Status change: `{ "toStatus": "REJECTED", "note": "Private property, not municipal" }`.
Correction: `{ "note": "Marked resolved by mistake" }`.

**Answers.**
A history row: `{ "id", "reportId", "fromStatus", "toStatus", "note", "correction", "sysCreatedAt", "sysCreatedBy" }`,
where `correction` is true for a correction.
The status history uses the platform list shape `{ data, meta }`.
The summary is a list of `{ "categoryCode", "status", "count" }`, one item per
combination that has at least one active report.

**List rules.** Sort keys: `id`, `observedOn`, `status`, `categoryCode`
(`sort=-id` is the default). `status` must be one of the four values and
`categoryCode` must be on the reference list; anything else is a 400.
`addressContains` matches part of the address, ignoring letter case.

**Messages** (the `detail` of the problem document):

| Situation | Status | `detail` |
| --- | --- | --- |
| empty description | 400 | `description: must not be blank` |
| description over 1000 characters | 400 | `description: size must be between 0 and 1000` |
| unknown category | 400 | `unknown categoryCode: POTHOLE` |
| date seen in the future | 400 | `observedOn must not be in the future` |
| address not in the register | 400 | `address not found in the address register` |
| unknown status value | 400 | `unknown status: OPEN` |
| status change without `toStatus` | 400 | `toStatus: must not be blank` |
| unknown sort key | 400 | `unknown sort key; sortable: categoryCode, id, observedOn, status` |
| rejection without a note | 400 | `note is required when rejecting` |
| correction without a note | 400 | `note is required for a correction` |
| no active report with that id | 404 | `fault report 1234 not found` |
| edit after work started | 409 | `a report can only be edited while it is New` |
| move not in the status table | 409 | `status cannot change from RESOLVED to IN_PROGRESS` |
| nothing to correct | 409 | `nothing to correct` |

The messages that start with a field name (`description: ...`, `toStatus: ...`)
are the platform's Bean Validation defaults, so they need no custom text.

### Address register  *(the X-Road part)*

`AddressRegistryAdapter.lookup(addressText)` is the port: one method, three
outcomes.

| Outcome | What the report gets |
| --- | --- |
| found | `address_code` = the code the register returned |
| not found | refused, 400 `address not found in the address register` |
| unreachable (no answer within 2 seconds, or a 5xx) | saved with `address_code` empty, shown as "address not verified" |

It is called on create, and on edit only when the address text changed.

**The imitated register.** `MockAddressRegistryController`, in the same shape
as the example's `MockOwnerRegistryController`: enabled by
`faults.address-registry.mock-server=true` (local profile only), it answers
`GET /mock-registry/addresses?text=<address>` with
`200 { "address": "...", "code": "..." }` or `404`. It compares the address
ignoring letter case and outer spaces, against four canned addresses:

| Address | Code |
| --- | --- |
| Ehitajate tee 5, Tallinn | ADS-0001 |
| Raekoja plats 1, Tartu | ADS-0002 |
| Pikk 12, Pärnu | ADS-0003 |
| Narva mnt 7, Tallinn | ADS-0004 |

The codes are made up for the course; they are not real address register
codes. The adapter calls the URL in `faults.address-registry.url` (locally
`http://localhost:18440/mock-registry`). To simulate an unreachable register,
point that URL at a port where nothing listens.

**Failure policy: fail open.** An unreachable register never blocks a report.
Reason: a fault report is a notice, not a decision about a person; losing the
report costs more than an unchecked address, and the officer can check it by
hand.

**X-Road.** Not part of the test assignment (lecturer, 16.10.2026): there is no
open X-Road access for students. The port leaves room for an X-Road transport
after the X-Road lecture, like the example's `XRoadOwnerRegistryAdapter`.

Possible later improvement (not in this course): a second address service as a
fallback, for example a map service. Left out because it is not on X-Road and
would send residents' locations to a third party.

## UI  *(→ session 4)*

| Screen | Element | Component | Rules |
| --- | --- | --- | --- |
| Report list `/faults` | table | `ResourceList` | columns: report number (a link to the report page), category, address, status, reported on, "not verified" mark; search by address; filters by status and category |
| | report button | `AppButtonPrimary` | "Report a problem", opens the form |
| | summary | `AppCard` | counts per status and category from `/summary` |
| Report form `/faults/new` | category | `Select` | required; options from `/categories` |
| | address | `Input` | required, ≤255 |
| | description | `Input.TextArea` | required, ≤1000 |
| | date seen | `DatePicker` | optional; no future dates |
| | submit | `AppButtonPrimary` | on success: notification with the report number, then the report page; on 400 the problem text is shown as it comes |
| | cancel | `AppButtonSecondary` | back to the list, nothing saved |
| Report page `/faults/{id}` | report | `ResourceForm` + `useDataController` | category, address, description, date seen, status; "address not verified" shown when `address_code` is empty |
| | edit | `AppButtonSecondary` "Edit" | shown only while New; switches the form to edit mode; save sends `PUT`; 409 text shown as it comes |
| | remove | `AppButtonSecondary` "Remove report" | asks for confirmation ("The report will be hidden from all lists. It stays in the database."); on 204, back to the list with a notification |
| | status history | `AppCard` | from `/status-changes`; one line per row: from, to, note, who, when; corrections marked "Undo" |
| | change status | `Select` + `Input.TextArea` + `AppButtonPrimary` | offers only the allowed next statuses; note required for Rejected; 409 text shown as it comes |
| | undo last change | `AppButtonSecondary` + `Input.TextArea` | shown only when the last row is an ordinary move; note required; asks for confirmation first; sends `POST /corrections` |
| | metadata | `ResourceForm.sidebar` — `AppCard` of `FieldItem`s | created at / by, modified at / by, version; read-only |

## Business tests

Users in the tests: `resident1` reports, `officer1` handles. "The register
knows" means the address is one of the four canned addresses above.

### Creating a report

1. **A valid report is accepted and starts as New.**
   Given category STREETLIGHT, address "Ehitajate tee 5, Tallinn" and a
   description, when `resident1` submits the report, then it is saved (201,
   `Location: /api/faults/reports/{id}`), its status is NEW, its address code is
   ADS-0001, it was created by `resident1`, and its history has one row: from
   empty to NEW, by `resident1`.
2. **A report needs a description.**
   Given a description that is empty or only spaces, when the report is
   submitted, then it is refused (400) with "description: must not be blank",
   and nothing is saved.
3. **A description has at most 1000 characters.**
   Given a description of 1001 characters, when the report is submitted, then
   it is refused (400) with "description: size must be between 0 and 1000".
   With exactly 1000 characters, it is accepted.
4. **The category must be on the list.**
   Given category POTHOLE, when the report is submitted, then it is refused
   (400) with "unknown categoryCode: POTHOLE".
5. **A fault cannot be seen in the future.**
   Given a date seen of tomorrow, when the report is submitted, then it is
   refused (400) with "observedOn must not be in the future". With today's
   date, it is accepted.
6. **An address the register does not know is refused.**
   Given the address "Nowhere 1, Tallinn", when the report is submitted, then it
   is refused (400) with "address not found in the address register", and
   nothing is saved.
7. **The address check ignores letter case and outer spaces.**
   Given the address "  ehitajate tee 5, tallinn ", when the report is
   submitted, then it is accepted with address code ADS-0001, and the address is
   stored as "ehitajate tee 5, tallinn".
8. **An unreachable address register does not block a report.**
   Given the address register does not answer, when a valid report is
   submitted, then it is saved (201) with an empty address code, and the report
   page shows "address not verified".

### Finding reports

9. **The list shows active reports, newest first, and filters combine.**
   Given three active reports (STREETLIGHT NEW, STREETLIGHT RESOLVED,
   ROAD_SURFACE NEW) and one retired STREETLIGHT NEW report, when the list is
   asked for with status NEW and category STREETLIGHT, then exactly one report
   comes back and `meta.total` is 1. Without filters, three reports come back,
   newest first.
10. **Address search ignores letter case.**
    Given a report at "Ehitajate tee 5, Tallinn", when the list is searched with
    `addressContains=EHITAJATE`, then that report is found.
11. **Unknown sort keys and filter values are refused.**
    When the list is asked for with `sort=colour`, then it is refused (400) with
    "unknown sort key; sortable: categoryCode, id, observedOn, status". With
    `status=OPEN`, it is refused (400) with "unknown status: OPEN".
12. **The summary counts active reports only.**
    Given the reports of test 9, when the summary is asked for, then it shows
    STREETLIGHT / NEW 1, STREETLIGHT / RESOLVED 1 and ROAD_SURFACE / NEW 1, and
    nothing else. The retired report is not counted.

### Editing and retiring

13. **A New report can be edited.**
    Given a NEW report, when `resident1` changes its description, then the change
    is saved (200), the status stays NEW, `sysVersion` goes up by one,
    `sysModifiedBy` is `resident1`, and the status history does not change.
14. **A report is locked once work starts.**
    Given an IN_PROGRESS report, when an edit is sent, then it is refused (409)
    with "a report can only be edited while it is New", and nothing changes.
15. **A changed address is checked again.**
    Given a NEW report at "Ehitajate tee 5, Tallinn", when the address is changed
    to "Pikk 12, Pärnu", then the address code becomes ADS-0003. When it is
    changed to "Nowhere 1, Tallinn" instead, the edit is refused (400) with
    "address not found in the address register", and the report keeps its old
    address and code.
16. **A retired report disappears but is kept.**
    Given an active report, when it is retired, then the answer is 204. After
    that, reading it answers 404, it is missing from the list and the summary,
    and in the database the row is still there with `sys_status` C. Retiring it
    again answers 404.

### Status and history

17. **A report moves forward, and every move is recorded.**
    Given a NEW report created by `resident1`, when `officer1` sets it to
    IN_PROGRESS and then to RESOLVED, then each change answers 201, the status is
    RESOLVED, and the history has three rows, oldest first: empty → NEW by
    `resident1`, NEW → IN_PROGRESS by `officer1`, IN_PROGRESS → RESOLVED by
    `officer1`.
18. **A status never skips, never goes back, and final statuses are final.**
    Given a RESOLVED report, when it is set to IN_PROGRESS, then the change is
    refused (409) with "status cannot change from RESOLVED to IN_PROGRESS", and
    the status stays RESOLVED. The same happens for a NEW report set to RESOLVED
    (work must start first), a NEW report set to NEW, and a REJECTED report set
    to any of the four statuses.
19. **A rejection needs a reason.**
    Given a NEW report, when `officer1` sets it to REJECTED without a note, then
    the change is refused (400) with "note is required when rejecting". With the
    note "Private property, not municipal", it is accepted and the note appears
    in the history.
20. **An unknown status is refused.**
    Given a NEW report, when it is set to OPEN, then the change is refused (400)
    with "unknown status: OPEN".
21. **A mistaken change can be undone, and the undo is kept.**
    Given a report moved from IN_PROGRESS to RESOLVED by mistake, when
    `officer1` undoes the last change with the note "Marked resolved by
    mistake", then the answer is 201, the status is IN_PROGRESS again, and the
    history shows both the change and the undo, with `correction` true on the
    undo. Without a note, the undo is refused (400) with "note is required for a
    correction".
22. **Only the last ordinary change can be undone.**
    Given a report whose only history row is empty → NEW, when an undo is sent,
    then it is refused (409) with "nothing to correct". Given a report whose last
    row is already an undo, a second undo is refused the same way.
23. **Undoing the start of work makes the report editable again.**
    Given a report moved from NEW to IN_PROGRESS, when the change is undone with
    a note, then the status is NEW, and an edit of the description is accepted
    (200).
24. **A retired report cannot be changed.**
    Given a retired report, when it is edited, set to a new status or undone,
    then each request answers 404.

### Access

25. **Every call needs a signed-in user.**
    When any `/api/faults` endpoint is called without an `Authorization` header,
    then the answer is 401 as `application/problem+json`.

## Decisions

- **Address register:** imitated inside the application. Confirmed by the
  lecturer on 16.10.2026; X-Road is not part of the test assignment.
- **Edit and retire** (added 18.10.2026, required by the REST API manual): edit
  only while New; retire at any status, no reason needed, because who and when
  are recorded by the platform.
- **Work split** (agreed 18.10.2026): Vlad: specification, business tests,
  address register adapter and its imitation, screens. Villu: database, REST
  API, and a usability review of the screens.
