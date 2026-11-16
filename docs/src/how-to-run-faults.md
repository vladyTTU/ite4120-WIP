# How to run FAULTS

The FAULTS component (public infrastructure fault reporting) runs inside this
course template, next to the animals example. This page is for anyone who wants
to start it on their own machine and try it in about five minutes.

> Status: the screens and part of the API are still being built. This page
> describes the finished component and is checked again before the
> presentation.

## 1. Before the first start

You need the same tools as the course template: Git, Docker Desktop, JDK 25,
Node.js 20 or newer, and a GitHub token with `read:packages`. The template's
`README.md`, section **Tools**, lists them and shows how to install them and the
token.

```bash
git clone https://github.com/vladyTTU/ite4120-WIP.git
cd ite4120-WIP
```

## 2. Start it

Start Docker Desktop first. Then use two terminals, both in the `ite4120-WIP`
folder.

| Step | macOS / Linux | Windows (PowerShell) |
| --- | --- | --- |
| 1. Database and backend | `./scripts/run-backend.sh` | `.\scripts\run-backend.ps1` |
| 2. Frontend, in a second terminal, after the backend says "Started Application" | `./scripts/run-frontend.sh` | `.\scripts\run-frontend.ps1` |

The first start downloads dependencies and takes a few minutes. You are signed
in automatically as a mock user.

| What | Where |
| --- | --- |
| The FAULTS screens | <http://localhost:18640/faults> |
| Swagger UI, the live API contract (section `faults`) | <http://localhost:18440/swagger-ui.html> |
| The imitated address register | <http://localhost:18440/mock-registry/addresses?text=Ehitajate%20tee%205,%20Tallinn> |
| The specification | `docs/src/specifications/FAULTS.01-fault-reporting.md` |

## 3. Try it in five minutes

1. Open <http://localhost:18640/faults>. The list shows the demo reports and
   the counts per category and status.
2. Click **Report a problem**. Choose *Streetlight*, type the address
   `Ehitajate tee 5, Tallinn` and a short description, and send it. The new
   report opens with status *New* and the address code *ADS-0001* from the
   address register.
3. Try again with the address `Nowhere 1, Tallinn`. The report is refused:
   the register does not know that address.
4. On your report's page, move it to *In progress*, then to *Resolved*. Then
   use **Undo last change** with a note: it goes back to *In progress*, and the
   history keeps both steps.
5. In Swagger UI, open the `faults` section: every endpoint, with GET, POST,
   PUT and DELETE, and the answers it can give.

To see the register fail without blocking anyone, start the backend with the
register pointed at a port where nothing listens, then send a report: it is
saved and marked "address not verified".

| macOS / Linux | Windows (PowerShell) |
| --- | --- |
| `FAULTS_ADDRESSREGISTRY_URL=http://localhost:1 ./scripts/run-backend.sh` | `$env:FAULTS_ADDRESSREGISTRY_URL="http://localhost:1"; .\scripts\run-backend.ps1` |

## 4. Check every screen

A full pass over the screens, one row per rule in FAULTS.01 § UI. Use it after
any change to the screens or the API, and before the presentation.

| # | Screen | Do | Expect |
| --- | --- | --- | --- |
| 1 | List | open <http://localhost:18640/faults> | the reports, newest first; the counts per category and status above the list |
| 2 | List | type `ehitajate` in the search field | only reports with that address; letter case does not matter |
| 3 | List | open the filter, choose status *New*, press Apply; then Clear all | only New reports; then all of them again |
| 4 | List | click a row (not the report number) | the detail panel: category, address with its code, description, status, and the register's answer "via the address register (mock / X-Road)" |
| 5 | List | open the column settings (gear) | *Date seen* is hidden at first and can be switched on; *Report no.* cannot be hidden or moved |
| 6 | List | stop the backend and reload the page | "Could not load reports" with a Try again button, never an empty list |
| 7 | Form | open **Report a problem** | Send report stays grey until category, address and description are filled |
| 8 | Form | open the date field | dates are shown as DD/MM/YYYY; no future day can be picked |
| 9 | Form | send the address `Nowhere 1, Tallinn` | the message "address not found in the address register"; nothing is saved |
| 10 | Form | send a valid report at `Ehitajate tee 5, Tallinn` | a notification with the report number, then the report page: status New (blue), address code ADS-0001 |
| 11 | Report page | Edit, change the description, Save | saved; the version in the sidebar goes up by one |
| 12 | Report page | change the status to *In progress*, then press Edit | the tag turns green; Edit is refused with "a report can only be edited while it is New" |
| 13 | Report page | choose *Rejected* | Change status stays grey until a reason is typed; once saved, the tag is red and the reason is in the history |
| 14 | Report page | on a report moved from In progress to Resolved, use Undo last change with a reason | back to In progress; the history shows the change and the undo, marked "Undo" |
| 15 | Report page | Remove report, and confirm | back to the list; the report is gone from the list and the counts |

## 5. Look at the database

The three FAULTS tables live in the schema `faults`: `fault_category`,
`fault_report` and `status_change`. Their SQL is in
`backend/src/main/resources/faults/db/changelog/faults/`.

From a terminal, open a database shell with `./scripts/psql.sh` (Windows:
`.\scripts\psql.ps1`) and type:

```sql
\dt faults.*
select id, category_code, address_text, address_code, status from faults.fault_report;
select report_id, from_status, to_status, note, sys_created_by from faults.status_change;
```

Or connect a database tool such as DBeaver: PostgreSQL, host `localhost`,
port `18520`, database `animals`, user `animals`, password `animals` (local
development values only).

## 6. Stop and start clean

Press **Ctrl+C** in both terminals. To start again from an empty database,
run `./scripts/reset-db.sh` (Windows: `.\scripts\reset-db.ps1`); the next
backend start rebuilds the tables and the demo data.
