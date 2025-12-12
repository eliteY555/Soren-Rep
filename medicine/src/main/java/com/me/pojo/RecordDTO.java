package com.me.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.me.utils.CustomDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDTO {
    private Integer recordId;
    private Integer patientId;
    private Integer doctorId;
    private String description;
    private String tongue;
    private Integer status;
    private String patientName;
    private String phone;
    private Integer sex;
    private Integer age;
    private String oldHistory;
    private String allergiesHistory;
    private String habits;
    private String doctorName;
    private String hospitalName;
    private String departmentName;
    private DiagnosticDTO diagnosticDTO;
    private Float score;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = CustomDateDeserializer.class)
    private Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date updateTime;
}