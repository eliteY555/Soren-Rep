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

    /** 患者注册：BCrypt 哈希 + 手机号跨表查重 */
    @PostMapping("/register")
    public Result register(@RequestBody Patient patient) {
        // 1. 必填校验
        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            return Result.error("手机号不能为空");
        }
        if (patient.getPassword() == null || patient.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (patient.getPatientName() == null || patient.getPatientName().trim().isEmpty()) {
            return Result.error("姓名不能为空");
        }

        // 2. 跨表查重（patient + doctor）
        if (patientService.isPhoneRegistered(patient.getPhone())) {
            return Result.error("该手机号已被注册");
        }

        // 3. BCrypt 哈希
        patient.setPassword(PasswordUtil.encode(patient.getPassword()));

        // 4. 写入
        try {
            patientService.register(patient);
            // 返回时清除密码字段
            patient.setPassword(null);
            return Result.success(patient);
        } catch (DataIntegrityViolationException e) {
            return Result.error("该手机号已被注册");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /** 患者登录：BCrypt 验证 */
    @PostMapping("/login")
    public Result login(@RequestBody java.util.Map<String, String> loginData) {
        String phone = loginData.get("phone");
        String password = loginData.get("password");

        if (phone == null || password == null) {
            return Result.error("手机号和密码不能为空");
        }

        Patient patient = patientService.findByPhone(phone);
        if (patient == null) {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!PasswordUtil.matches(password, patient.getPassword())) {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        // 登录成功，清除密码再返回
        patient.setPassword(null);
        return Result.success(patient);
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
            e.printStackTrace();
            return Result.error("创建失败: " + e.getMessage());
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
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
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
            e.printStackTrace();
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
