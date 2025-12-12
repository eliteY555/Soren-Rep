package com.me.controller;

import com.me.common.Result;
import com.me.pojo.Patient;
import com.me.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/patient")
public class PatientController {
    @Autowired
    private PatientService patientService;

    @PostMapping("/create")
    public Result createPatientInfo(@RequestBody Patient patient) {
        try {
            System.out.println("创建患者信息请求: " + patient);
            if (patient.getUserId() == null) {
                return Result.error("患者ID不能为空");
            }
            
            int i = patientService.createPatientInfo(patient);
            if (i > 0) {
                return Result.success(patient);
            } else {
                return Result.error("创建失败，请稍后再试");
            }
        } catch (Exception e) {
            System.out.println("创建患者信息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("创建失败，请稍后再试: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result updatePatientInfo(@RequestBody Patient patient) {
        try {
            System.out.println("更新患者信息请求: " + patient);
            if (patient.getUserId() == null) {
                return Result.error("患者ID不能为空");
            }
            
            int i = patientService.updatePatientInfo(patient);
            if (i > 0) {
                return Result.success(true);
            } else {
                return Result.error("更新失败，请稍后再试");
            }
        } catch (Exception e) {
            System.out.println("更新患者信息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("更新失败，请稍后再试: " + e.getMessage());
        }
    }

    @GetMapping("/get/{userId}")
    public Result getPatientByUserId(@PathVariable("userId") Integer userId) {
        try {
            System.out.println("获取患者信息请求，userId: " + userId);
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }
            
            Patient patient = patientService.getPatientByUserId(userId);
            
            if (patient == null) {
                System.out.println("未找到患者信息，创建默认对象: Patient(userId=" + userId + ", patientId=null, patientName=null, sex=0, age=null, oldHistory=null, allergiesHistory=null, habits=null)");
                // 如果患者信息为空，则返回空对象而不是null
                patient = new Patient();
                patient.setUserId(userId);
                patient.setSex(0); // 默认性别为男
            }
            
            return Result.success(patient);
        } catch (Exception e) {
            System.out.println("获取患者信息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取失败，请稍后再试: " + e.getMessage());
        }
    }
}
