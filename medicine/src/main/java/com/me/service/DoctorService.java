package com.me.service;

import com.me.pojo.Doctor;
import com.me.pojo.DoctorDTO;

import java.util.List;
import java.util.Map;

public interface DoctorService {
    int updateDoctorInfo(Doctor doctor);

    Doctor getDoctorById(Integer doctorId);

    List<Doctor> queryAll();

    Map<String, Object> queryDoctorList(DoctorDTO doctorDTO);

    Doctor findByPhone(String phone);

    Doctor register(Doctor doctor);

    int updateAccount(Doctor doctor);

    /** 检查手机号是否已在医生或患者表中注册 */
    boolean isPhoneRegistered(String phone);
}
