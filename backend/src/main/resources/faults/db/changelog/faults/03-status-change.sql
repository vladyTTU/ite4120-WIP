--liquibase formatted sql

--changeset ite4120:faults-03-status-change
--comment Auditable history of report status changes, including undo
create table faults.status_change (
    id                  bigint default nextval('core.seq_id') not null,
    report_id           bigint not null,
    from_status         text,
    to_status           text not null,
    note                text,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint status_change_pk primary key (id),
    constraint status_change_report_fk foreign key (report_id) references faults.fault_report (id),
    constraint status_change_from_chk check (from_status is null or from_status in ('NEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    constraint status_change_to_chk check (to_status in ('NEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    constraint status_change_note_chk check (note is null or length(note) <= 500),
    constraint status_change_rejected_note_chk check (to_status <> 'REJECTED' or length(btrim(coalesce(note, ''))) > 0)
);

select core.create_table_metadata('faults.status_change');

create index status_change_report_idx on faults.status_change (report_id) where (sys_status = 'A');
--
