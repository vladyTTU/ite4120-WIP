--liquibase formatted sql

--changeset ite4120:faults-90-demo-data context:demo
--comment Demo fault report with initial NEW history entry
/* Sample address is illustrative; the mock address register must be configured separately. */
insert into faults.fault_report (category_code, address_text, address_code, description, observed_on, status)
values ('STREETLIGHT', 'Ehitajate tee 5, Tallinn', null, 'Streetlight is not working', '2026-10-01', 'NEW');

insert into faults.status_change (report_id, from_status, to_status, note)
select id, null, 'NEW', null
from faults.fault_report
where description = 'Streetlight is not working'
  and address_text = 'Ehitajate tee 5, Tallinn';
--
