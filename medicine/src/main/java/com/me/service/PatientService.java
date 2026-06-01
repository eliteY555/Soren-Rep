package com.me.service;

import com.me.pojo.Patient;

public interface PatientService {
    int updatePatientInfo(Patient patient);

    int createPatientInfo(Patient patient);

    Patient getPatientById(Integer patientId);

    Patient findByPhone(String phone);

    Patient register(Patient patient);

    int updateAccount(Patient patient);

    /** 检查手机号是否已在患者或医生表中注册 */
    boolean isPhoneRegistered(String phone);
}
