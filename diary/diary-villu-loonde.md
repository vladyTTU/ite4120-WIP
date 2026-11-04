# Work Diary – Villu Loonde

**Course:** ITE4120 – Introduction to IT and eGov Technologies  
**Semester:** Autumn 2026  
**Group component:** FAULTS – Public Infrastructure Fault Reporting

## Work Log

| Date | Hours | Type | Work completed | What I asked the AI agent | What the AI got wrong | How I corrected or verified it | Commit / PR |
|---|---:|---|---|---|---|---|---|
| 01.10.2026 | 0.50 | Independent | Reviewed course requirements, assessment criteria and individual contribution expectations. | Explain course deliverables and assessment requirements. | | Compared explanations with course instructions. | |
| 01.10.2026 | 0.50 | Independent | Prepared my GitHub account and learned about repositories, branches, commits and pull requests. | Explain GitHub collaboration in simple terms. | | Checked the instructions against GitHub. | |
| 01.10.2026 | 0.25 | Independent | Installed and verified Docker Desktop, including Hello World and Docker Compose. | Guide installation and explain Docker. | | Verified successful command execution. | |
| 01.10.2026 | 1.00 | Independent | Studied BABOK user-story principles and prepared two alternative public-service scenarios, including infrastructure fault reporting. | Help structure user stories, acceptance criteria and possible technical components. | | Compared the scenarios and selected fault reporting as a feasible proposal. | |
| 01.10.2026 | 0.75 | Independent | Studied basic IT concepts, including frontend, backend, APIs, servers, databases and system architecture. | Explain technical concepts using practical examples. | | Related the concepts to the FAULTS service. | |
| 01.10.2026 | 0.75 | Independent | Read the mandatory TEHIK materials to understand the development guidelines and non-functional requirements. | Explain unfamiliar technical terminology. | | Referred to the original course materials. | |
| 02–13.10.2026 | 0.50 | Independent | Contributed the original infrastructure fault-reporting concept, subsequently developed into the group's FAULTS user story and specification. | Compare project alternatives and clarify the scope. | | Reviewed the selected project direction. | `FAULTS-US-001` |
| 18.10.2026 | 0.75 | Independent | Reviewed the FAULTS specification and user story, including validation, status changes and error handling. | Explain requirements and assess the proposed user journey. | | Compared the specification with the original proposal and group decisions. | PR #1 |
| 18.10.2026 | 0.50 | Independent | Reviewed team responsibilities, clarified Vlad's X-Road responsibility and offered to contribute to UI usability and customer experience. | Explain how the technical components and responsibilities connect. | | Clarified responsibilities in the GitHub review discussion. | PR #1 |
| 18.10.2026 | 0.75 | Independent | Reviewed the FAULTS pull request and learned the review, approval and merge process. | Guide GitHub pull-request review and merging. | | Checked the pull request and merge status in GitHub. | PR #1 |
| 18.10.2026 | 1.00 | Independent | Set up GitHub Desktop, cloned the team repository, created the `add-villu-diary` branch and prepared my personal Markdown diary. | Guide repository setup, branch selection and file creation. | | Verified the local repository structure and branch. | Diary PR #2 (merged) |
| 18.10.2026 | 0.25 | Independent | Read the Development Workflow document to understand specification-first development, business tests, pull requests and diary requirements. | Explain the development workflow. | | Checked the explanation against the course document. | |
| 18.10.2026 | 0.25 | Independent | Read the Liquibase Guide and familiarised myself with changesets and database migrations. | Explain Liquibase and database versioning. | | Compared the explanation with the course guide. | |
| 18.10.2026 | 0.25 | Independent | Read the REST API Manual to understand controllers, services, repositories, validation and API documentation. | Explain the REST API architecture. | | Checked the explanation against the manual. | |
| 18.10.2026 | 0.25 | Independent | Read the Database Manual to understand PostgreSQL, tables, foreign keys and migration requirements. | Explain the database architecture. | | Checked the explanation against the manual. | |
| 18.10.2026 | 0.25 | Independent | Read the Frontend Manual to understand React, the Helex UI library, Storybook and frontend requirements. | Explain the frontend structure and its relation to FAULTS. | | Compared the explanation with the manual. | |
| 18.10.2026 | 2.50 | Independent | Designed and implemented the FAULTS PostgreSQL schema and Liquibase migrations, including categories, reports, status-change records and demo data. Integrated migrations into the master changelog. | Explain SQL migration structure, changesets and how to validate them. | AI-proposed file locations and migration integration required checking against the actual repository and existing changelog. | Validated changesets and confirmed the Spring Boot backend started with the migrations. | PR #4, `1432695` |
| 21.10.2026 | 3.50 | Independent | Implemented the FAULTS REST API for report creation, retrieval, updating and soft retirement, with business validation and a simulated address-registry integration. | Provide complete Java files and guide local compilation, startup and API testing. | AI-generated Java code required integration with the existing repository and validation against actual build and API responses. | Installed and verified Microsoft OpenJDK; compiled with Gradle, ran PostgreSQL in Docker and Spring Boot locally, and tested CRUD operations through curl/Swagger. Verified create (201), read/update (200), soft-retire (204) and retired-record lookup (404), with mock address lookup. | PR #4, `fbc31f5` |
| 04.11.2026 | 2.50 | Independent | Extended the FAULTS API with category listing, report listing, category filtering, sorting, pagination and report summary counts by category and status. | Help implement controller, service and repository changes and provide test commands. | Report listing initially returned HTTP 405 and the first summary request returned HTTP 400; controller mappings and implementation needed revision. | Retested category listing, listing, filtering, pagination and summary; confirmed HTTP 200 and expected counts for three active reports. Verified invalid future observation date was rejected with HTTP 400. | PR #4, `b372533` |
| 04.11.2026 | 1.50 | Independent | Implemented status-change endpoints and persistent audit history for fault reports, including status transition notes and actor information. | Generate full Java files and guide compilation and endpoint verification. | AI-generated files needed compilation and real HTTP verification; passing compilation alone would not prove audit persistence. | Java compilation succeeded; POST changed report 1060 from NEW to IN_PROGRESS; GET returned the saved audit record with note and creator. | PR #4, `918bb32` |
| 04.11.2026 | 0.75 | Independent | Committed and pushed FAULTS development to the team repository, updated the existing pull request and reviewed GitHub Actions checks. | Guide GitHub Desktop commit/push workflow, PR review and CI verification. | Initially checked the earlier commit's green checks rather than the latest commit. | Selected commit `918bb32` in GitHub and confirmed backend Gradle tests, frontend build and database grammar checks were green. Left PR open for teammate review. | PR #4 |

## Time-estimation method

The original 8.50 hours are retained exactly as previously recorded. The five development entries below are **retrospective estimates**, reconstructed on 04.11.2026 from dated Git commits, PR activity, terminal/API results, and the development conversations. They are **not measured session durations**: chat timestamps and commit timestamps do not establish all active work time. The estimates allocate 2.50 h to the database/Liquibase work, 3.50 h to CRUD/address validation and local setup, 2.50 h to listing/filtering/summary and troubleshooting, 1.50 h to status changes and history, and 0.75 h to commits/CI review. These estimates should be adjusted if personal time records differ. No hours are claimed for pending work.

## Reflection and Next Steps

The FAULTS implementation was developed with AI assistance for explanations, code drafts and test commands. I integrated the proposed changes into the team repository, used local compilation and HTTP requests to verify behaviour, and checked CI results for the latest commit rather than assuming earlier successful checks covered it. The simulated address-registry lookup is not a live X-Road integration. The green Gradle job confirms the repository test job passed, not that the planned FAULTS-specific test suite is complete.

**Pending work (not completed):** corrections and undo behaviour, additional FAULTS-specific automated tests, final documentation, and teammate review/merge of PR #4.

## Time Summary

| Type | Hours |
|---|---:|
| Previously recorded study and project work | **8.50** |
| Reconstructed development work (estimated) | **10.75** |
| **Total (recorded + estimated)** | **19.25** |

*Of the total, 8.50 hours were recorded in the original diary and 10.75 hours are retrospective estimates, not independently verified time measurements.*
