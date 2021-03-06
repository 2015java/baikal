package com.baikal.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author kowalski
 */
@SpringBootApplication
//@ComponentScan({ "com.baikal.client", "com.baikal.test" })
public class BaikalTestApplication {
  public static void main(String... args) {
    SpringApplication.run(BaikalTestApplication.class, args);
  }
}
