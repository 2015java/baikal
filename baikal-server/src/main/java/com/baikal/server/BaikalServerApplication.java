package com.baikal.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author kowalski
 */
@SpringBootApplication
@EnableScheduling
@EnableRabbit
@MapperScan(basePackages = "com.baikal.dao")
@EnableTransactionManagement
public class BaikalServerApplication {
  public static void main(String... args) {
    SpringApplication.run(BaikalServerApplication.class, args);
  }
}