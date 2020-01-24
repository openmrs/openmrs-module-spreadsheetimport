alter table tr_hiv_enrollment drop column patient_id;
alter table tr_demographics drop column patient_id;
alter table tr_triage drop column patient_id;
alter table tr_hts_initial drop column patient_id;
alter table tr_hts_retest drop column patient_id;
alter table tr_hiv_regimen_history drop column patient_id;
alter table tr_hiv_followup drop column patient_id;

alter table tr_hiv_enrollment add column patient_id int(100) default null;
alter table tr_triage add column patient_id int(100) default null;
alter table tr_hts_initial add column patient_id int(100) default null;
alter table tr_hts_retest add column patient_id int(100) default null;
alter table tr_hiv_regimen_history add column patient_id int(100) default null;
alter table tr_hiv_followup add column patient_id int(100) default null;
alter table tr_hiv_program_enrollment add column patient_id int(100) default null;

alter table tr_hiv_enrollment add index (Person_Id);
alter table tr_triage add index (Person_Id);
alter table tr_hts_initial add index (Person_Id);
alter table tr_hts_retest add index (Person_Id);
alter table tr_hiv_regimen_history add index (Person_Id);
alter table tr_hiv_followup add index (Person_Id);
alter table tr_hiv_program_enrollment add index (Person_Id);


update migration_tr.tr_triage a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hiv_followup a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hiv_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hts_retest a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hiv_regimen_history a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hts_initial a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;
update migration_tr.tr_hiv_program_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier and b.identifier_type=16 set a.patient_id = b.patient_id;


