
 -- Add patient_id
alter table migration_tr.tr_hiv_enrollment add column patient_id int(100) default null;
alter table migration_tr.tr_hiv_program_enrollment add column patient_id int(100) default null;
alter table migration_tr.tr_hiv_program_discontinuation add column patient_id int(100) default null;
alter table migration_tr.tr_triage add column patient_id int(100) default null;
alter table migration_tr.tr_hts_initial add column patient_id int(100) default null;
alter table migration_tr.tr_hts_retest add column patient_id int(100) default null;
alter table migration_tr.tr_ipt_enrollment add column patient_id int(100) default null;
alter table migration_tr.tr_ipt_program add column patient_id int(100) default null;
alter table migration_tr.tr_ipt_outcome add column patient_id int(100) default null;
alter table migration_tr.tr_ipt_followup add column patient_id int(100) default null;
alter table migration_tr.tr_hiv_followup add column patient_id int(100) default null;
alter table migration_tr.tr_hiv_regimen_history add column patient_id int(100) default null;
--alter table migration_tr.tr_alcohol_screening add column patient_id int(100) default null;
alter table migration_tr.tr_enhanced_adherence add column patient_id int(100) default null;
alter table migration_tr.tr_mch_enrollment add column patient_id int(100) default null;
alter table migration_tr.tr_defaulter_tracing add column patient_id int(100) default null;
alter table migration_tr.tr_otz_activity add column patient_id int(100) default null;
alter table migration_tr.tr_otz_outcome add column patient_id int(100) default null;
alter table migration_tr.tr_ovc_enrolment add column patient_id int(100) default null;
alter table migration_tr.tr_ovc_outcome add column patient_id int(100) default null;
alter table migration_tr.tr_otz_enrolment add column patient_id int(100) default null;
alter table migration_tr.tr_hts_linkage add column patient_id int(100) default null;
alter table migration_tr.tr_mch_discharge add column patient_id int(100) default null;
alter table migration_tr.tr_mch_delivery add column patient_id int(100) default null;
alter table migration_tr.tr_mch_anc_visit add column patient_id int(100) default null;
alter table migration_tr.tr_mch_pnc_visit add column patient_id int(100) default null;
alter table migration_tr.tr_hei_outcome add column patient_id int(100) default null;
alter table migration_tr.tr_hei_enrollment add column patient_id int(100) default null;
alter table migration_tr.tr_hei_followup add column patient_id int(100) default null;
alter table migration_tr.tr_person_relationship add column Person_a_person_id INT(11) default null, add column person_b_person_id INT(11) default null, add column openmrs_relationship_type INT(11) default null;

--Add index
alter table migration_tr.tr_hiv_enrollment add index (Person_Id);
alter table migration_tr.tr_hiv_program_enrollment add index (Person_Id);
alter table migration_tr.tr_hiv_program_discontinuation add index (Person_Id);
alter table migration_tr.tr_triage add index (Person_Id);
alter table migration_tr.tr_hts_initial add index (Person_Id);
alter table migration_tr.tr_hts_retest add index (Person_Id);
alter table migration_tr.tr_ipt_enrollment add index (Person_Id);
alter table migration_tr.tr_ipt_program add index (Person_Id);
alter table migration_tr.tr_ipt_outcome add index (Person_Id);
alter table migration_tr.tr_ipt_followup add index (Person_Id);
alter table migration_tr.tr_hiv_followup add index (Person_Id);
alter table migration_tr.tr_hiv_regimen_history add index (Person_Id);
--alter table migration_tr.tr_alcohol_screening add index (Person_Id);
alter table migration_tr.tr_enhanced_adherence add index (Person_Id);
alter table migration_tr.tr_mch_enrollment add index (Person_Id);
alter table migration_tr.tr_defaulter_tracing add index (Person_Id);
alter table migration_tr.tr_otz_activity add index (Person_Id);
alter table migration_tr.tr_otz_outcome add index (Person_Id);
alter table migration_tr.tr_ovc_enrolment add index (Person_Id);
alter table migration_tr.tr_ovc_outcome add index (Person_Id);
alter table migration_tr.tr_otz_enrolment add index (Person_Id);
alter table migration_tr.tr_hts_linkage add index (Person_Id);
alter table migration_tr.tr_mch_discharge add index (Person_Id);
alter table migration_tr.tr_mch_delivery add index (Person_Id);
alter table migration_tr.tr_mch_anc_visit add index (Person_Id);
alter table migration_tr.tr_mch_pnc_visit add index (Person_Id);
alter table migration_tr.tr_hei_outcome add index (Person_Id);
alter table migration_tr.tr_hei_enrollment add index (Person_Id);
alter table migration_tr.tr_hei_followup add index (Person_Id);
alter table migration_tr.tr_person_relationship add index(Index_Person_Id);
alter table migration_tr.tr_person_relationship add index(Relative_Person_Id);

update migration_tr.tr_hiv_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hiv_program_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hiv_program_discontinuation a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_triage a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hts_initial a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hts_retest a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ipt_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ipt_program a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ipt_outcome a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ipt_followup a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hiv_followup a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hiv_regimen_history a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

--update migration_tr.tr_alcohol_screening a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
--inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where --uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set --a.patient_id = b.patient_id;

update migration_tr.tr_enhanced_adherence a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_mch_enrollment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_defaulter_tracing a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_otz_activity a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_otz_outcome a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ovc_enrolment a inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_ovc_outcome a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_otz_enrolment a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hts_linkage a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_mch_discharge a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_mch_delivery a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_mch_anc_visit a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;


update migration_tr.tr_mch_pnc_visit a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hei_outcome a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_hei_enrollment a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;


update migration_tr.tr_hei_followup a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_vital_labs a
inner join openmrs.patient_identifier b on a.Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.patient_id = b.patient_id;

update migration_tr.tr_person_relationship a
inner join openmrs.patient_identifier b on a.Index_Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.Person_a_person_id = b.patient_id;

update migration_tr.tr_person_relationship a
inner join openmrs.patient_identifier b on a.Relative_Person_Id = b.identifier
inner join (select patient_identifier_type_id from openmrs.patient_identifier_type where uuid="b3d6de9f-f215-4259-9805-8638c887e46b") pt on pt.patient_identifier_type_id = b.identifier_type set a.person_b_person_id = b.patient_id;

