--liquibase formatted sql

--changeset ite4120:faults-91-demo-reports context:demo
--comment Demo reports in every status, with their history and address codes
/* The first demo report (90-demo-data.sql) gets its address code from the
   imitated register. The new reports cover every status, so a fresh start shows
   the whole workflow. The last one has no code: it shows how a report looks
   when the register was down ("address not verified").
   Narva mnt 7 is left out on purpose: the business tests use that address. */
update faults.fault_report
   set address_code = 'ADS-0001'
 where description = 'Streetlight is not working'
   and address_text = 'Ehitajate tee 5, Tallinn'
   and address_code is null;

insert into faults.fault_report (category_code, address_text, address_code, description, observed_on, status)
values ('ROAD_SURFACE', 'Raekoja plats 1, Tartu', 'ADS-0002', 'Deep pothole in front of the town hall', '2026-10-05', 'IN_PROGRESS'),
       ('TRAFFIC_SIGN', 'Pikk 12, Pärnu', 'ADS-0003', 'Stop sign bent over after the storm', '2026-10-08', 'RESOLVED'),
       ('PLAYGROUND', 'Raekoja plats 1, Tartu', 'ADS-0002', 'Swing seat is broken', '2026-10-12', 'REJECTED'),
       ('PAVEMENT', 'Ehitajate tee 5, Tallinn', null, 'Loose paving stones at the bus stop', '2026-10-20', 'NEW');

insert into faults.status_change (report_id, from_status, to_status, note)
select id, 'NEW', 'IN_PROGRESS', 'Repair crew booked'
from faults.fault_report
where description = 'Deep pothole in front of the town hall';

insert into faults.status_change (report_id, from_status, to_status, note)
select id, 'NEW', 'IN_PROGRESS', null
from faults.fault_report
where description = 'Stop sign bent over after the storm';

insert into faults.status_change (report_id, from_status, to_status, note)
select id, 'IN_PROGRESS', 'RESOLVED', 'Sign replaced'
from faults.fault_report
where description = 'Stop sign bent over after the storm';

insert into faults.status_change (report_id, from_status, to_status, note)
select id, 'NEW', 'REJECTED', 'Private playground, not municipal'
from faults.fault_report
where description = 'Swing seat is broken';
--
