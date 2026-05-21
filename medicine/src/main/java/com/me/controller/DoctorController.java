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

    // 医生注册
    @PostMapping("/register")
    public Result register(@RequestBody Doctor doctor) {
        try {
            String password = PasswordUtil.desEncrypt(doctor.getPassword());
            doctor.setPassword(password);
            Doctor saved = doctorService.register(doctor);
            return Result.success(saved);
        } catch (DataIntegrityViolationException e) {
            System.out.println("注册约束冲突: " + e.getMessage());
            return Result.error("用户名或手机号已存在，或必填字段为空");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    // 医生登录
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String phone = loginData.get("phone");
        String password = PasswordUtil.desEncrypt(loginData.get("password"));
        Doctor doctor = doctorService.login(phone, password);
        if (doctor != null) {
            return Result.success(doctor);
        } else {
            return Result.error(ResultEnum.USERNAME_OR_PASSWORD_ERROR);
        }
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
            System.out.println("创建医生信息错误: " + e.getMessage());
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
            System.out.println(e.getMessage());
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
            System.out.println("获取医生信息异常，doctorId: " + doctorId);
            e.printStackTrace();
            return Result.error("查询失败，请稍后再试: " + e.getMessage());
        }
    }

    @GetMapping
    public Result queryAll() {
        List<Doctor> doctor = doctorService.queryAll();
        return Result.success(doctor);
    }

    @PostMapping("/page")
    public Result queryDoctors(@RequestBody DoctorDTO doctorDTO) {
        Map<String, Object> result = doctorService.queryDoctorList(doctorDTO);
        return Result.success(result);
    }
}
