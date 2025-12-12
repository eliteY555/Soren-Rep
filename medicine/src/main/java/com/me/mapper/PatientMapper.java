package com.me.mapper;

import com.me.pojo.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper {
    int updatePatientInfo(Patient patient);

    int insertPatientInfo(Patient patient);

    Patient getPatientByUserId(Integer userId);
}
