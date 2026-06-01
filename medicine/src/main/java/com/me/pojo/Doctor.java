package com.me.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Doctor {
    private Integer doctorId;
    private String doctorName;
    private String password;
    private String phone;
    private String email;
    private String cityName;
    private String hospitalName;
    private String departmentName;
    private String introduction;
}
