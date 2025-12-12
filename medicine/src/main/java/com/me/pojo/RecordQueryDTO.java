package com.me.pojo;

import lombok.Data;

@Data
public class RecordQueryDTO {
    private String status;       // 病历状态
    private Integer patientId;   // 患者ID
    private String doctorId;    // 医生姓名
    private Integer page = 1;     // 当前页码
    private Integer pageSize = 5;// 每页条数
}
