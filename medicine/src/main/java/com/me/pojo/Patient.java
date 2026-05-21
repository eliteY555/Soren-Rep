package com.me.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Patient {
    private Integer patientId;
    private String patientName;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer sex;
    private Integer age;
    private String oldHistory;
    private String allergiesHistory;
    private String habits;
}
