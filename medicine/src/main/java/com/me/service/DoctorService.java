package com.me.service;

import com.me.pojo.Doctor;
import com.me.pojo.DoctorDTO;

import java.util.List;
import java.util.Map;

public interface DoctorService {
    int updateDoctorInfo(Doctor doctor);

    Doctor getDoctorByUserId(Integer userId);

    List<Doctor> queryAll();

    Map<String, Object> queryDoctorList(DoctorDTO doctorDTO);
}
