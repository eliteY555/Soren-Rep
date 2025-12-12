package com.me.tools;

import com.me.pojo.Record;
import com.me.pojo.User;
import com.me.service.RecordService;
import com.me.service.UserService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private UserService userService;

    /**
     * 提交病例工具
     * 如果患者ID不存在，会尝试自动创建患者
     * @param record 病例记录对象
     * @return 操作结果信息
     */
    @Tool(name = "提交病例", value = "根据参数，在确保所需数据通过对话得到后，为用户执行病例的提交")
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
            
            // 如果没有患者ID，尝试通过手机号查找患者
            if (record.getPatientId() == null) {
                User existingUser = userService.getUserByIdentity(record.getPhone());
                
                if (existingUser != null) {
                    // 找到了患者，使用其ID
                    record.setPatientId(existingUser.getUserId());
                    logger.info("通过手机号找到患者: {}", existingUser.getUserId());
                } else {
                    // 创建新患者
                    User newUser = new User();
                    newUser.setUsername(record.getPatientName());
                    newUser.setPhone(record.getPhone());
                    newUser.setPassword("123456"); // 默认密码
                    newUser.setRole(0); // 患者角色
                    
                    try {
                        int userId = userService.add(newUser);
                        record.setPatientId(userId);
                        logger.info("自动创建患者成功，ID: {}", userId);
                    } catch (Exception e) {
                        logger.error("创建患者失败", e);
                        return "提交失败：无法创建患者账户，" + e.getMessage();
                    }
                }
            }
            
            // 检查患者是否存在
            if (userService.queryById(record.getPatientId()) == null) {
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
}
