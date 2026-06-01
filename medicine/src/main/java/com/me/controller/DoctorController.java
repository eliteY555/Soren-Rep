package com.me.controller;

import com.me.common.Result;
import com.me.common.ResultEnum;
import com.me.pojo.Doctor;
import com.me.pojo.DoctorDTO;
import com.me.service.DoctorService;
import com.me.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/doctor")
public class DoctorController {
    @Autowired
    private DoctorService doctorService;

    /** 医生注册：BCrypt 哈希 + 手机号跨表查重 */
    @PostMapping("/register")
    public Result register(@RequestBody Doctor doctor) {
        // 1. 必填校验
        if (doctor.getPhone() == null || doctor.getPhone().trim().isEmpty()) {
            return Result.error("手机号不能为空");
        }
        if (doctor.getPassword() == null || doctor.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (doctor.getDoctorName() == null || doctor.getDoctorName().trim().isEmpty()) {
            return Result.error("姓名不能为空");
        }

        // 2. 跨表查重（doctor + patient）
        if (doctorService.isPhoneRegistered(doctor.getPhone())) {
            return Result.error("该手机号已被注册");
        }

        // 3. BCrypt 哈希
        doctor.setPassword(PasswordUtil.encode(doctor.getPassword()));

        // 4. 写入
        try {
            Doctor saved = doctorService.register(doctor);
            if (saved != null) saved.setPassword(null);
            return Result.success(saved);
        } catch (DataIntegrityViolationException e) {
            return Result.error("该手机号已被注册");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /** 医生登录：BCrypt 验证 */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String phone = loginData.get("phone");
        String password = loginData.get("password");

        if (phone == null || password == null) {
            return Result.error("手机号和密码不能为空");
        }

        Doctor doctor = doctorService.findByPhone(phone);
        if (doctor == null) {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!PasswordUtil.matches(password, doctor.getPassword())) {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }

        doctor.setPassword(null);
        return Result.success(doctor);
    }

    @PostMapping("/create")
    public Result createDoctorInfo(@RequestBody Doctor doctor) {
        try {
            int i = doctorService.updateDoctorInfo(doctor);
            if (i > 0) {
                return Result.success(doctor);
            } else {
                return Result.error("医生信息创建失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("医生信息创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result updateDoctorInfo(@RequestBody Doctor doctor) {
        try {
            int i = doctorService.updateDoctorInfo(doctor);
            if (i > 0) {
                return Result.success(true);
            } else {
                return Result.error("更新失败，请稍后再试");
            }
        } catch (Exception e) {
            return Result.error("更新失败，请稍后再试");
        }
    }

    @GetMapping("/get/{doctorId}")
    public Result getDoctorById(@PathVariable("doctorId") Integer doctorId) {
        try {
            if (doctorId == null) {
                return Result.error("医生ID不能为空");
            }
            Doctor doctor = doctorService.getDoctorById(doctorId);
            if (doctor == null) {
                doctor = new Doctor();
                doctor.setDoctorId(doctorId);
            }
            return Result.success(doctor);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping
    public Result queryAll() {
        List<Doctor> doctors = doctorService.queryAll();
        return Result.success(doctors);
    }

    @PostMapping("/page")
    public Result queryDoctors(@RequestBody DoctorDTO doctorDTO) {
        Map<String, Object> result = doctorService.queryDoctorList(doctorDTO);
        return Result.success(result);
    }
}
