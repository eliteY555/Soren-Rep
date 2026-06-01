package com.me.service.impl;

import com.me.mapper.DoctorMapper;
import com.me.mapper.PatientMapper;
import com.me.pojo.Doctor;
import com.me.pojo.DoctorDTO;
import com.me.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorServiceImpl implements DoctorService {
    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Override
    public int updateDoctorInfo(Doctor doctor) {
        return doctorMapper.updateDoctorInfo(doctor);
    }

    @Override
    public Doctor getDoctorById(Integer doctorId) {
        return doctorMapper.getDoctorById(doctorId);
    }

    @Override
    public List<Doctor> queryAll() {
        return doctorMapper.queryAll();
    }

    @Override
    public Map<String, Object> queryDoctorList(DoctorDTO doctorDTO) {
        int offset = (doctorDTO.getPage() - 1) * doctorDTO.getPageSize();

        Map<String, Object> params = new HashMap<>();
        params.put("cityName", doctorDTO.getCityName());
        params.put("hospitalName", doctorDTO.getHospitalName());
        params.put("departmentName", doctorDTO.getDepartmentName());
        params.put("offset", offset);
        params.put("pageSize", doctorDTO.getPageSize());

        List<Doctor> doctorList = doctorMapper.selectDoctorList(params);
        int total = doctorMapper.countDoctorList(params);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("doctorList", doctorList);
        return result;
    }

    @Override
    public Doctor findByPhone(String phone) {
        return doctorMapper.findByPhone(phone);
    }

    @Override
    public Doctor register(Doctor doctor) {
        doctorMapper.updateDoctorInfo(doctor);
        return doctorMapper.findByPhone(doctor.getPhone());
    }

    @Override
    public int updateAccount(Doctor doctor) {
        return doctorMapper.updateAccount(doctor);
    }

    @Override
    public boolean isPhoneRegistered(String phone) {
        return doctorMapper.findByPhone(phone) != null
                || patientMapper.findByPhone(phone) != null;
    }
}
