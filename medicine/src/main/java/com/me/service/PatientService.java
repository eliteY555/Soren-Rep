package com.me.service;

import com.me.pojo.Patient;

public interface PatientService {
    int updatePatientInfo(Patient patient);

    int createPatientInfo(Patient patient);

    Patient getPatientById(Integer patientId);

    Patient findByPhone(String phone);

    Patient login(String phone, String password);

    Patient register(Patient patient);

    int updateAccount(Patient patient);
}
