# FAULTS-US-001 · Report a public infrastructure fault

> **FAULTS-US-001** · state **Proposed** · traces to [FAULTS.01](../specifications/FAULTS.01-fault-reporting.md)

**As** a municipality resident
**I want** to report a public infrastructure fault by giving its location and details
**so that** the responsible municipal team can identify, track and resolve it,
and I can see what happened to my report.

## Context

Today a broken streetlight or a pothole is reported by phone or e-mail, and the
resident never learns what happened. A driver reports a pothole so it gets fixed
before it damages cars; a resident reports a broken streetlight so the street
feels safe at night.

This component gives each report a number, a category, a checked address and a
status history. The address is checked against the national address register
over X-Road (a local imitation in this course). The application keeps only the
address code the register returns, never the register's own data.

The reporter is the signed-in user, recorded by the platform's `sys_created_by`
column. No personal code, phone number or e-mail is stored, because the service
does not need them. All reports are visible to every signed-in user, so a
resident can see that a fault is already reported instead of filing it twice.

Users: the **resident**, who reports a fault and follows it; the **municipal
officer**, who searches reports, moves them through statuses and sees the
counts; and the **address register** (external), which confirms an address.

Out of scope: photos, maps and coordinates, e-mail or SMS notices, routing a
report to a specific authority, real authentication (mock auth only).

Idea and first user story (two options, 1 October 2026): Villu Loonde. This
story develops Option 1.

## Acceptance criteria

- A report can be created with a category, an address, a description and an
  optional date seen.
- Obvious nonsense is refused with a clear message: unknown category, empty
  description, date seen in the future, address the register does not know.
- An unreachable address register never blocks a report (fail open); the report
  is flagged as "address not verified".
- The details of a report can be corrected while it is still New. Once an
  officer starts work, the report is locked.
- A duplicate or test report can be removed from all lists; it stays in the
  database.
- Every new report starts as New, and every status change is kept in a history.
- A status only moves forward: New → In progress → Resolved, or
  New / In progress → Rejected. Resolved and Rejected are final. If a fixed
  fault comes back, the resident files a new report.
- A rejection needs a reason. An officer can undo the last status change to
  correct a mistake; the undo also needs a reason and stays in the history.
- The list can be filtered by status and category and searched by address text.
- The officer can see the number of active reports per category and status.
