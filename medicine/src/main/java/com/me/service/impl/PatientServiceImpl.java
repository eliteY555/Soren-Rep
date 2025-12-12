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
        System.out.println("更新患者信息: " + patient);
        if (patient.getUserId() == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        return patientMapper.updatePatientInfo(patient);
    }

    @Override
    public int createPatientInfo(Patient patient) {
        System.out.println("创建患者信息: " + patient);
        if (patient.getUserId() == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        return patientMapper.insertPatientInfo(patient);
    }

    @Override
    public Patient getPatientByUserId(Integer userId) {
        Patient patient = patientMapper.getPatientByUserId(userId);
        System.out.println("查询结果: " + patient);
        return patient;
    }
}
