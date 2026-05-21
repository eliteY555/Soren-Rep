package com.me.mapper;

import com.me.pojo.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PatientMapper {
    int insertPatientInfo(Patient patient);

    int updatePatientInfo(Patient patient);

    Patient getPatientById(Integer patientId);

    Patient findByPhone(String phone);

    Patient findByPhoneAndPassword(@Param("phone") String phone, @Param("password") String password);

    int updateAccount(Patient patient);
}
