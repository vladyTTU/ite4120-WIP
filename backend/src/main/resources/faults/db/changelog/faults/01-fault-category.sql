--liquibase formatted sql

--changeset ite4120:faults-01-fault-category
--comment Reference list of public infrastructure fault categories
create table faults.fault_category (
    code                text not null,
    name                text not null,
    constraint fault_category_pk primary key (code)
);
--

--changeset ite4120:faults-01-fault-category-seed
--comment Seed fault categories
insert into faults.fault_category (code, name) values
    ('STREETLIGHT',  'Streetlight'),
    ('ROAD_SURFACE', 'Road surface'),
    ('TRAFFIC_SIGN', 'Traffic sign'),
    ('PAVEMENT',     'Pavement'),
    ('PLAYGROUND',   'Playground'),
    ('OTHER',        'Other')
on conflict do nothing;
--
