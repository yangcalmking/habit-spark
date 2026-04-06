package com.habitspark;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.habitspark.dao")
public class HabitSparkApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitSparkApplication.class, args);
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  🌟 HabitSpark 启动成功！");
        System.out.println("  学生端: http://localhost:8080/student");
        System.out.println("  家长端: http://localhost:8080/parent");
        System.out.println("  健康检查: http://localhost:8080/actuator/health");
        System.out.println("═══════════════════════════════════════════");
    }
}
