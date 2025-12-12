package com.me.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

//使用@Data自动生成需要的get、set
@Data
//使用@AllArgsConstructor自动生成有参构造
@AllArgsConstructor
//使用@NoArgsConstructor自动生成无参构造
@NoArgsConstructor
public class User {
    private Integer userId;
    private String username;
    private String password;
    private String phone;
    private String email;
    // 0：患者；1：医生
    private Integer role;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date createTime;
    
    private Integer sex;
    private Integer age;
    private String avatar;
    private Integer status;
    
    private String hospitalName;
    private String departmentName;
    private String introduction;
    private String title;
    private String address;
    private String idCard;
}
