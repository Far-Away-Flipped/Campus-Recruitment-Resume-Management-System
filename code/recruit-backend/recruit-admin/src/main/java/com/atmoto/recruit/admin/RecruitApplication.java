package com.atmoto.recruit.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园招聘管理系统 —— 启动入口
 * <p>
 * 遨天科技（北京）有限公司 | 人文发展部
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.atmoto.recruit")
@MapperScan("com.atmoto.recruit.**.mapper")
public class RecruitApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园招聘简历管理系统 启动完成");
        System.out.println("  遨天科技（北京）有限公司");
        System.out.println("========================================");
    }
}
