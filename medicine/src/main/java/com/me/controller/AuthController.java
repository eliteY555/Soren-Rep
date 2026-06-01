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
 * 先查 patient 表，再查 doctor 表，BCrypt 验证密码
 */
@RestController
@RequestMapping(value = "/auth")
public class AuthController {
    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    /** 统一登录：手机号 + BCrypt 密码验证 */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String identity = loginData.get("identity"); // 手机号
        String password = loginData.get("password");

        if (identity == null || password == null) {
            return Result.error("手机号和密码不能为空");
        }

        // 先在 patient 表中查找
        Patient patient = patientService.findByPhone(identity);
        if (patient != null && PasswordUtil.matches(password, patient.getPassword())) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", patient.getPatientId());
            userInfo.put("username", patient.getPatientName());
            userInfo.put("phone", patient.getPhone());
            userInfo.put("email", patient.getEmail());
            userInfo.put("role", 0);
            return Result.success(userInfo);
        }

        // 再在 doctor 表中查找
        Doctor doctor = doctorService.findByPhone(identity);
        if (doctor != null && PasswordUtil.matches(password, doctor.getPassword())) {
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

    /** 修改资料（支持修改密码） */
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

            if ("0".equals(String.valueOf(role))) {
                Patient patient = patientService.getPatientById(userId);
                if (patient == null) return Result.error("患者不存在");

                // BCrypt 验证旧密码
                if (oldPassword != null && !oldPassword.isEmpty()) {
                    if (!PasswordUtil.matches(oldPassword, patient.getPassword())) {
                        return Result.error("原密码错误");
                    }
                    if (newPassword != null && !newPassword.isEmpty()) {
                        patient.setPassword(PasswordUtil.encode(newPassword));
                    }
                }
                patient.setPatientName(username);
                patient.setPhone(phone);
                patient.setEmail(email);
                patientService.updateAccount(patient);
            } else {
                Doctor doctor = doctorService.getDoctorById(userId);
                if (doctor == null) return Result.error("医生不存在");

                if (oldPassword != null && !oldPassword.isEmpty()) {
                    if (!PasswordUtil.matches(oldPassword, doctor.getPassword())) {
                        return Result.error("原密码错误");
                    }
                    if (newPassword != null && !newPassword.isEmpty()) {
                        doctor.setPassword(PasswordUtil.encode(newPassword));
                    }
                }
                doctor.setDoctorName(username);
                doctor.setPhone(phone);
                doctor.setEmail(email);
                doctorService.updateAccount(doctor);
            }
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
