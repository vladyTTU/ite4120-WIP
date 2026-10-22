--liquibase formatted sql

--changeset ite4120:faults-schema
--comment Create the faults component schema
create schema if not exists faults;
--
