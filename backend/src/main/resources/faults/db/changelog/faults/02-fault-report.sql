--liquibase formatted sql

--changeset ite4120:faults-02-fault-report
--comment Main business table for public infrastructure fault reports
/* Validate observed_on <= today in the API (a moving date is not a stable DB constraint).
   The address register may be unavailable: address_code stays null until verified.
   Platform sys_* values are populated by the metadata trigger. */
create table faults.fault_report (
    id                  bigint default nextval('core.seq_id') not null,
    category_code       text not null,
    address_text        text not null,
    address_code        text,
    description         text not null,
    observed_on         date,
    status              text not null default 'NEW',
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint fault_report_pk primary key (id),
    constraint fault_report_category_fk foreign key (category_code) references faults.fault_category (code),
    constraint fault_report_address_chk check (length(btrim(address_text)) between 1 and 255),
    constraint fault_report_description_chk check (length(btrim(description)) between 1 and 1000),
    constraint fault_report_status_chk check (status in ('NEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'))
);

select core.create_table_metadata('faults.fault_report');

create index fault_report_category_idx on faults.fault_report (category_code) where (sys_status = 'A');
create index fault_report_status_idx on faults.fault_report (status) where (sys_status = 'A');
--
