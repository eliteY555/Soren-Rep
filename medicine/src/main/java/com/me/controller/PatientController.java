package com.me.controller;

import com.me.common.Result;
import com.me.common.ResultEnum;
import com.me.pojo.Patient;
import com.me.service.PatientService;
import com.me.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/patient")
public class PatientController {
    @Autowired
    private PatientService patientService;

    // 患者注册
    @PostMapping("/register")
    public Result register(@RequestBody Patient patient) {
        try {
            String password = PasswordUtil.desEncrypt(patient.getPassword());
            patient.setPassword(password);
            patientService.register(patient);
            return Result.success(patient);
        } catch (DataIntegrityViolationException e) {
            System.out.println("注册约束冲突: " + e.getMessage());
            return Result.error("用户名或手机号已存在，或必填字段为空");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    // 患者登录
    @PostMapping("/login")
    public Result login(@RequestBody java.util.Map<String, String> loginData) {
        String phone = loginData.get("phone");
        String password = PasswordUtil.desEncrypt(loginData.get("password"));
        Patient patient = patientService.login(phone, password);
        if (patient != null) {
            return Result.success(patient);
        } else {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    @PostMapping("/create")
    public Result createPatientInfo(@RequestBody Patient patient) {
        try {
            if (patient.getPatientId() == null) {
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
            if (patient.getPatientId() == null) {
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

    @GetMapping("/get/{patientId}")
    public Result getPatientById(@PathVariable("patientId") Integer patientId) {
        try {
            if (patientId == null) {
                return Result.error("患者ID不能为空");
            }
            Patient patient = patientService.getPatientById(patientId);
            if (patient == null) {
                patient = new Patient();
                patient.setPatientId(patientId);
                patient.setSex(0);
            }
            return Result.success(patient);
        } catch (Exception e) {
            System.out.println("获取患者信息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取失败，请稍后再试: " + e.getMessage());
        }
    }
}
