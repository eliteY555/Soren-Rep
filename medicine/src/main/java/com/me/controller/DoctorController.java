package com.me.controller;

import com.me.common.Result;
import com.me.pojo.Doctor;
import com.me.pojo.DoctorDTO;
import com.me.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/doctor")
public class DoctorController {
    @Autowired
    private DoctorService doctorService;

    @PostMapping("/create")
    public Result createDoctorInfo(@RequestBody Doctor doctor) {
        try {
            if (doctor.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }
            
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

    @GetMapping("/get/{userId}")
    public Result getDoctorByUserId(@PathVariable("userId") Integer userId) {
        try {
            System.out.println("正在获取医生信息，userId: " + userId);
            if (userId == null) {
                System.out.println("获取医生信息失败: userId为空");
                return Result.error("用户ID不能为空");
            }
            
            Doctor doctor = doctorService.getDoctorByUserId(userId);
            
            if (doctor == null) {
                System.out.println("未找到医生信息，userId: " + userId);
                // 如果医生信息为空，则返回空对象而不是null
                doctor = new Doctor();
                doctor.setUserId(userId);
            } else {
                System.out.println("成功获取医生信息: " + doctor);
            }
            
            return Result.success(doctor);
        } catch (Exception e) {
            System.out.println("获取医生信息异常，userId: " + userId);
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
