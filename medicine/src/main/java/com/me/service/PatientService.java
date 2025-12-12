package com.me.service;

import com.me.pojo.Patient;

public interface PatientService {
    int updatePatientInfo(Patient patient);
    
    int createPatientInfo(Patient patient);

    Patient getPatientByUserId(Integer userId);
}
