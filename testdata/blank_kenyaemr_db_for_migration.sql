set foreign_key_checks=0;
truncate table kenyaemr_hiv_testing_client_trace;
truncate table kenyaemr_hiv_testing_patient_contact;
truncate table muzima_queue_data;
truncate table muzima_registration_data;
truncate table muzimaregistration_registration_data;
truncate table reporting_report_request;

truncate table patient_identifier;
truncate table patient_program;
truncate table patient;

truncate table person_address;
truncate table person_attribute;
truncate table person_merge_log;
delete from person_name where person_id>1;
alter table person_name AUTO_INCREMENT=0;
truncate table relationship;

delete from person where person_id>1;
alter table person AUTO_INCREMENT=0;
truncate table provider_attribute;


truncate table test_order;
truncate table drug_order;
truncate table orders;
truncate table encounter;
truncate table obs;
truncate table visit;
truncate table user_property;
truncate table provider;
delete from user_role where user_id not in(select user_id from users where system_id='admin');
alter table users AUTO_INCREMENT=0;
delete from users where system_id not in('admin','daemon');
alter table users AUTO_INCREMENT=0;

SET FOREIGN_KEY_CHECKS=1;
