package com.me.tools;

import com.me.pojo.Patient;
import com.me.pojo.Record;
import com.me.service.PatientService;
import com.me.service.RecordService;
import com.me.utils.PasswordUtil;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Date;

/**
 * function calling-数据库操作
 */

@Component
public class AgentTools {
    private static final Logger logger = LoggerFactory.getLogger(AgentTools.class);

    @Autowired
    private RecordService recordService;

    @Autowired
    private PatientService patientService;

    /**
     * 提交病例工具
     * 如果患者不存在，会尝试自动创建患者
     * @param record 病例记录对象
     * @return 操作结果信息
     */
    @Tool(name = "submit_medical_record", value = "Submit a medical record for the patient. Collect all required information through conversation before calling this tool.")
    public String upLoadRecord(Record record) {
        try {
            logger.info("开始提交病例，病例信息: {}", record);

            if (record == null) {
                logger.error("病例信息为空");
                return "提交失败：病例信息不能为空";
            }

            // 验证必要字段
            if (record.getPatientName() == null || record.getPatientName().trim().isEmpty()) {
                logger.error("患者姓名为空");
                return "提交失败：患者姓名不能为空";
            }

            if (record.getPhone() == null || record.getPhone().trim().isEmpty()) {
                logger.error("手机号码为空");
                return "提交失败：手机号码不能为空";
            }

            // 设置创建时间
            if (record.getCreateTime() == null) {
                record.setCreateTime(new Date());
            }

            // 如果没有患者ID，尝试通过手机号查找或创建患者
            if (record.getPatientId() == null) {
                Patient existingPatient = patientService.findByPhone(record.getPhone());

                if (existingPatient != null) {
                    record.setPatientId(existingPatient.getPatientId());
                    logger.info("通过手机号找到患者: {}", existingPatient.getPatientId());
                } else {
                    // 创建新患者
                    Patient newPatient = new Patient();
                    newPatient.setPatientName(record.getPatientName());
                    newPatient.setPhone(record.getPhone());
                    newPatient.setEmail(record.getPhone() + "@temp.com");
                    newPatient.setPassword(PasswordUtil.encode(generateRandomPassword()));
                    newPatient.setSex(record.getSex());
                    newPatient.setAge(record.getAge());
                    newPatient.setOldHistory(record.getOldHistory());
                    newPatient.setAllergiesHistory(record.getAllergiesHistory());
                    newPatient.setHabits(record.getHabits());

                    try {
                        patientService.register(newPatient);
                        record.setPatientId(newPatient.getPatientId());
                        logger.info("自动创建患者成功，ID: {}", newPatient.getPatientId());
                    } catch (Exception e) {
                        logger.error("创建患者失败", e);
                        return "提交失败：无法创建患者账户，" + e.getMessage();
                    }
                }
            }

            // 检查患者是否存在
            if (patientService.getPatientById(record.getPatientId()) == null) {
                logger.error("患者不存在，患者ID: {}", record.getPatientId());
                return "提交失败：患者不存在，请先注册";
            }

            // 设置默认状态
            if (record.getStatus() == null) {
                record.setStatus(1); // 待诊断状态
            }

            // 提交病例
            int result = recordService.add(record);
            if (result > 0) {
                logger.info("病例提交成功，病例ID: {}", record.getRecordId());
                return "病例提交成功，病例ID: " + record.getRecordId();
            } else {
                logger.error("病例提交失败");
                return "病例提交失败，请稍后重试";
            }
        } catch (Exception e) {
            logger.error("病例提交过程中发生异常", e);
            return "病例提交失败：" + e.getMessage();
        }
    }

    /** 生成 12 位随机安全密码 */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
