package com.me.mapper;

import com.me.pojo.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DoctorMapper {
    int updateDoctorInfo(Doctor doctor);

    Doctor getDoctorById(Integer doctorId);

    List<Doctor> queryAll();

    List<Doctor> selectDoctorList(Map<String, Object> params);

    int countDoctorList(Map<String, Object> params);

    Doctor findByPhone(String phone);

    Doctor findByPhoneAndPassword(@Param("phone") String phone, @Param("password") String password);

    int updateAccount(Doctor doctor);
}
