# Work diary · Vlad Florin Vîță

ITE4120 Introduction to IT and eGov Technologies, autumn 2026 · group component `FAULTS` (public infrastructure fault reporting)

Budget: 137 hours of independent work (6 ECTS = 156 h, of which 19 h contact study). Session 2 and 3 slides: about 22 hours before 16.10 and 22 hours before 30.10.

| Date | Hours | Type | What I did | What I asked the agent | What it got wrong | How I corrected it | Commit / PR |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 18.09.2026 | 3 | Contact | Session 1: course intro, documentation and business use cases | | | | |
| 26.09.2026 | 1 | Independent | Installed Docker Desktop. It would not start until I installed WSL 2 (Linux inside Windows). | Why Docker Desktop would not open, and how to fix it. I shared screenshots of how it looked and that it was taking a very long time to open. | | | |
| 26.09.2026 | 0.1 | Independent | Created a GitHub account with my real name | | | | |
| 01.10.2026 | 0.5 | Independent | Read and discussed the two-option user story written by Villu Loonde | | | | |
| 02.10.2026 | 2.9 | Contact | Session 2: containers, PostgreSQL, Liquibase | | | | |
| 11.10.2026 | 3 | Independent | Reviewed the session 2 recording and slides; clean transcript and study notes | Clean the session 2 transcript and write study notes | | | |
| 11.10.2026 | 0.5 | Independent | Read the assessment criteria; made a catch-up plan | Map the deliverables against the dates and say where to start | | | |
| 11.10.2026 | 1 | Independent | Group chose Option 1; AI drafted the FAULTS.01 specification | Draft the specification for Option 1 in the course template's shape | | | |
| 11.10.2026 | 1 | Independent | Created the group repository (fork of the course template), installed GitHub Desktop, cloned it, opened pull request #1 with this diary | Step-by-step guidance on forking, cloning and opening a pull request | Told me to click "Use this template", but the course repository has no such button | Used Fork instead | PR #1 |
| 13.10.2026 | 1 | Independent | Reviewed the user story part of the FAULTS specification; decided the open questions; added the spec files to the repository | Guide the review; turn the spec into the template's Markdown files | The draft had no way to fix a mistaken status change, and did not say what happens when a fixed fault comes back | Added an undo as a recorded correction; a recurring fault needs a new report; decided no e-mail and all reports visible; credited Villu for the idea | PR #1 |
| 13.10.2026 | 0.5 | Independent | Set up the backend on my laptop: installed JDK 25, created a GitHub token, ran the template app and checked Swagger and the mock registry. PowerShell blocked the script at first; fixed by allowing local scripts (execution policy). | Step-by-step guidance on installing Java, the token and starting the backend | Gave a wrong total for the diary (9.1 instead of 8.6) | Checked the sum and corrected it | |
| 16.10.2026 | 3 | Contact | Session 3: REST APIs (HTTP verbs, status codes, resources instead of actions, idempotency), Swagger, Postman, business tests. Asked the lecturer which address service we can use: students have no open X-Road access, so we imitate the address register. | Live explanations of the slides and demos during the lecture | | | |
| 17.10.2026 | 3 | Independent | Clean transcript and study notes for session 3 | Clean the session 3 transcript and write study notes, in the same structure as session 2 | | | |
| 18.10.2026 | 0.4 | Independent | Synced our fork with the lecturer's template updates, reviewed and merged Villu's diary, deleted merged branches, switched on the automatic checks (GitHub Actions) | Why Villu's pull request showed three extra files, and how to remove them | | | PR #2 |
| 18.10.2026 | 0.6 | Independent | Updated the specification after session 3. Decided: a report can be edited only while New; it can be retired at any time without a reason; we keep the address check with an imitated register. | Revise the specification against the REST API manual and the lecture | The first draft followed only the assessment criteria: it had no edit or retire operation, which the REST API manual requires, and it used a verb in a path (/status/undo). It also did not warn me that GitHub Desktop opens the pull request against the lecturer's repository. | Added edit (PUT) and retire (DELETE), renamed the paths to /status-changes and /corrections, raised the business tests from 8 to 25; caught the wrong target before submitting and opened the pull request in our group repository | PR #3 |
| 22.10.2026 | 1 | Independent | Merged the specification update (PR #3). Posted a review of Villu's first API code (PR #4) listing what does not match the specification yet: paths, the list, categories and summary endpoints, the Location header, the date check, messages, Swagger and automated tests; asked him to leave the address register to me. Installed Node.js and ran the frontend on my laptop to prepare for session 4. | Compare Villu's code with the specification and the assessment criteria and draft the review; step-by-step guidance on installing Node.js and running the frontend | The review text it drafted made GitHub turn @RequestMapping, @Operation and @ApiResponse into mentions of GitHub users | Edited the comment and put the three names in code formatting | PR #3, PR #4 |
| 22.10.2026 | 0.3 | Independent | Built the imitated address register, my part of the X-Road criterion: GET /mock-registry/addresses answers with the address code (ADS-0001 to ADS-0004) or 404. Tested a known address, the same address in lower case with extra spaces, and an unknown address in the browser. Opened PR #5 for Villu's review. | Write the imitated register to match the specification; guide me through adding, testing and committing it | Its test links broke at the first space, so the browser sent only part of the address and every test answered 404 | Opened the links with %20 for the spaces; all three answers were then correct | PR #5 |

## Totals

| | Hours |
| --- | --- |
| Independent, logged | 13.9 |
| Contact, logged | 8.9 |
| Independent, left of 137 | 123.1 |