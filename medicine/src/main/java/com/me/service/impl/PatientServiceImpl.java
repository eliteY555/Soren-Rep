package com.me.service.impl;

import com.me.mapper.PatientMapper;
import com.me.pojo.Patient;
import com.me.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {
    @Autowired
    private PatientMapper patientMapper;

    @Override
    public int updatePatientInfo(Patient patient) {
        if (patient.getPatientId() == null) {
            throw new IllegalArgumentException("patientId不能为空");
        }
        return patientMapper.updatePatientInfo(patient);
    }

    @Override
    public int createPatientInfo(Patient patient) {
        return patientMapper.insertPatientInfo(patient);
    }

    @Override
    public Patient getPatientById(Integer patientId) {
        return patientMapper.getPatientById(patientId);
    }

    @Override
    public Patient findByPhone(String phone) {
        return patientMapper.findByPhone(phone);
    }

    @Override
    public Patient login(String phone, String password) {
        return patientMapper.findByPhoneAndPassword(phone, password);
    }

    @Override
    public Patient register(Patient patient) {
        patientMapper.insertPatientInfo(patient);
        return patient;
    }

    @Override
    public int updateAccount(Patient patient) {
        return patientMapper.updateAccount(patient);
    }
}
