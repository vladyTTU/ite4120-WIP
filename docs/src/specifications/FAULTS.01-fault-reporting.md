# FAULTS.01 · Fault reporting

> **FAULTS.01** · state **Proposed** · traces from [FAULTS-US-001](../user-stories/FAULTS-US-001.md)

## Description

A register of fault reports for public infrastructure: a resident reports a
fault, a municipal officer tracks it to a decision. Create, search, view and
change the status of a report, undo a mistaken status change, and show counts
by status and category. The location is checked against the national address
register through an adapter, as the worked example does for owners.

Excluded: photos and attachments, maps and coordinates, e-mail or SMS notices,
routing a report to a specific authority, real authentication (mock auth only),
any personal data beyond the platform `sys_*` columns.

## Data model  *(→ session 2)*

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `faults.fault_category` | `code` PK | text | reference list: STREETLIGHT, ROAD_SURFACE, TRAFFIC_SIGN, PAVEMENT, PLAYGROUND, OTHER |
| | `name` | text | display name, required |
| `faults.fault_report` | `id` PK | bigint | from `core.seq_id`; shown to the resident as the report number |
| | `category_code` | text | required; FK → `fault_category` |
| | `address_text` | text | required, ≤255; the address as the resident typed it |
| | `address_code` | text | optional; the code the address register returned; empty means "not verified" |
| | `description` | text | required, not blank, ≤1000 |
| | `observed_on` | date | optional; **not in the future** |
| | `status` | text | required; one of NEW, IN_PROGRESS, RESOLVED, REJECTED; starts as NEW |
| | `sys_*` | — | platform columns, trigger-managed; `sys_created_by` = the reporter |
| `faults.status_change` | `id` PK | bigint | from `core.seq_id` |
| | `report_id` | bigint | required; FK → `fault_report` |
| | `from_status` | text | empty for the first entry |
| | `to_status` | text | required; same four values as `status` |
| | `note` | text | optional, ≤500; **required when `to_status` is REJECTED, and for an undo** |
| | `sys_*` | — | platform columns; `sys_created_at` / `sys_created_by` = when and who |

Three tables in schema `faults`, two foreign keys. `fault_category` is a
reference table like `species`: natural key, no `sys_*` columns, its own seed
changeset. Reports are never deleted; `sys_status` goes from A to C. An undo is
a `status_change` row that moves backwards, with its reason in `note`.

Planned changesets: `faults-schema.sql`, `01-fault-category.sql`,
`02-fault-report.sql`, `03-status-change.sql`, `90-demo-data.sql` (`context:demo`).

## API  *(→ session 3)*

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/faults/categories` | the reference list | — |
| `POST /api/faults/reports` | create a report; checks the address; answers **201 + Location** | 400 validation (incl. address not found) |
| `GET /api/faults/reports` | paged search: `status`, `categoryCode`, `addressContains`, `limit`, `offset`, `sort` | 400 unknown sort key |
| `GET /api/faults/reports/{id}` | one active report with its status history | 404 |
| `POST /api/faults/reports/{id}/status` | move the report to a new status, with an optional note | 400 missing note on rejection · 404 · 409 transition not allowed |
| `POST /api/faults/reports/{id}/status/undo` | undo the last status change, with a required note; the previous status comes back | 400 missing note · 404 · 409 nothing to undo |
| `GET /api/faults/reports/summary` | count of active reports per category and status | — |

Every error is `application/problem+json`. All endpoints require
`Authorization: Bearer <user>` (mock auth). The create operation is
`POST /api/faults/reports`; the reporting operation is `GET /api/faults/reports/summary`.

**X-Road: address check.** `AddressRegistryAdapter.lookup(addressText)` is
called once, when a report is created. Found: the code is stored in
`address_code`. Not found: the report is refused, 400 "address not found in the
address register". In this course the register is imitated inside the
application, like `MockOwnerRegistryController`.

**Failure policy: fail open.** An unreachable register never blocks a report:
it is saved with `address_code` empty and shown as "address not verified".
Reason: a fault report is a notice, not a decision about a person; losing the
report costs more than an unchecked address, and the officer can check it by
hand.

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
| Report page `/faults/{id}` | report | `ResourceForm` + `useDataController`, view mode | category, address, description, date seen, status; "address not verified" shown when `address_code` is empty |
| | status history | `AppCard` | one line per change: from, to, note, who, when |
| | change status | `Select` + `Input.TextArea` + `AppButtonPrimary` | offers only the allowed next statuses; note required for Rejected; 409 text shown as it comes |
| | undo last change | `AppButtonSecondary` + `Input.TextArea` | shown only when there is a change to undo; note required; asks for confirmation first |
| | metadata | `ResourceForm.sidebar` — `AppCard` of `FieldItem`s | created at / by, modified at / by, version; read-only |

## Business tests

1. **A valid report is accepted and starts as New.**
   Given category STREETLIGHT, address "Ehitajate tee 5, Tallinn" known to the
   register and a description, when the report is submitted, then it is saved
   (201), its status is NEW, its address code is filled, and its history has one
   entry: from empty to NEW.
2. **A report needs a description.**
   Given a report with an empty description, when it is submitted, then it is
   refused (400) with a message naming `description`, and nothing is saved.
3. **A fault cannot be seen in the future.**
   Given a report with a date seen of tomorrow, when it is submitted, then it is
   refused (400) with "observedOn must not be in the future".
4. **An unreachable address register does not block a report.**
   Given the address register does not answer, when a valid report is submitted,
   then it is saved (201) with an empty address code, and the report page shows
   "address not verified".
5. **An address the register does not know is refused.**
   Given the register answers "not found" for the address, when the report is
   submitted, then it is refused (400) with "address not found in the address
   register".
6. **A status only moves forward.**
   Given a report with status RESOLVED, when an officer sets it to IN_PROGRESS,
   then the change is refused (409), and the status stays RESOLVED.
7. **A rejection needs a reason.**
   Given a report with status NEW, when an officer sets it to REJECTED without a
   note, then the change is refused (400); with the note "Private property, not
   municipal", it is accepted and the note appears in the history.
8. **A mistaken status change can be undone, and the undo is kept.**
   Given a report moved from IN_PROGRESS to RESOLVED by mistake, when the officer
   undoes the last change with the note "Marked resolved by mistake", then the
   status is IN_PROGRESS again, and the history shows both the change and the
   undo. Without a note, the undo is refused (400).

## Open questions

- Which address service does the course's X-Road test environment offer? Ask the
  lecturer on 16.10.
- Who owns which part. Proposal: Vlad takes the specification, business tests and
  screens; Villu takes the database, API and X-Road.
