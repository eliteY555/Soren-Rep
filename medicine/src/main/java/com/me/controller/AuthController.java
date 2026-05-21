package com.me.controller;

import com.me.common.Result;
import com.me.common.ResultEnum;
import com.me.pojo.Doctor;
import com.me.pojo.Patient;
import com.me.service.DoctorService;
import com.me.service.PatientService;
import com.me.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一认证控制器
 * 根据 phone+password 分别在 patient 表和 doctor 表中查找，匹配任意一方即登录成功
 */
@RestController
@RequestMapping(value = "/auth")
public class AuthController {
    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String identity = loginData.get("identity"); // 手机号
        String password = PasswordUtil.desEncrypt(loginData.get("password"));

        // 先在 patient 表中查找
        Patient patient = patientService.login(identity, password);
        if (patient != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", patient.getPatientId());
            userInfo.put("username", patient.getPatientName());
            userInfo.put("phone", patient.getPhone());
            userInfo.put("email", patient.getEmail());
            userInfo.put("role", 0);
            return Result.success(userInfo);
        }

        // 再在 doctor 表中查找
        Doctor doctor = doctorService.login(identity, password);
        if (doctor != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", doctor.getDoctorId());
            userInfo.put("username", doctor.getDoctorName());
            userInfo.put("phone", doctor.getPhone());
            userInfo.put("email", doctor.getEmail());
            userInfo.put("role", 1);
            return Result.success(userInfo);
        }

        return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
    }

    @GetMapping("/findPassword")
    public Result findPassword(@RequestParam String identity) {
        Patient patient = patientService.findByPhone(identity);
        if (patient != null) {
            return Result.success(patient.getPatientId());
        }
        Doctor doctor = doctorService.findByPhone(identity);
        if (doctor != null) {
            return Result.success(doctor.getDoctorId());
        }
        return Result.error("用户不存在");
    }

    @PostMapping("/update")
    public Result update(@RequestBody Map<String, String> data) {
        try {
            Integer userId = Integer.parseInt(data.get("userId"));
            String username = data.get("username");
            String phone = data.get("phone");
            String email = data.get("email");
            String role = data.get("role");
            String oldPassword = data.get("oldPassword");
            String newPassword = data.get("newPassword");

            if ("0".equals(role) || "0".equals(String.valueOf(role))) {
                // 更新 patient 表
                Patient patient = patientService.getPatientById(userId);
                if (patient == null) {
                    return Result.error("患者不存在");
                }
                // 验证旧密码
                if (oldPassword != null && !oldPassword.isEmpty()) {
                    String decryptedOld = PasswordUtil.desEncrypt(oldPassword);
                    if (!patient.getPassword().equals(decryptedOld)) {
                        return Result.error("原密码错误");
                    }
                }
                patient.setPatientName(username);
                patient.setPhone(phone);
                patient.setEmail(email);
                if (newPassword != null && !newPassword.isEmpty()) {
                    patient.setPassword(PasswordUtil.desEncrypt(newPassword));
                }
                patientService.updateAccount(patient);
            } else {
                // 更新 doctor 表
                Doctor doctor = doctorService.getDoctorById(userId);
                if (doctor == null) {
                    return Result.error("医生不存在");
                }
                if (oldPassword != null && !oldPassword.isEmpty()) {
                    String decryptedOld = PasswordUtil.desEncrypt(oldPassword);
                    if (!doctor.getPassword().equals(decryptedOld)) {
                        return Result.error("原密码错误");
                    }
                }
                doctor.setDoctorName(username);
                doctor.setPhone(phone);
                doctor.setEmail(email);
                if (newPassword != null && !newPassword.isEmpty()) {
                    doctor.setPassword(PasswordUtil.desEncrypt(newPassword));
                }
                doctorService.updateAccount(doctor);
            }
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
