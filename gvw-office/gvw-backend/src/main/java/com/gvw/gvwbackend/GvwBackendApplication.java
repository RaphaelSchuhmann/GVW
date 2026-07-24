package com.gvw.gvwbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    excludeName = {
      "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
      "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    })
public class GvwBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(GvwBackendApplication.class, args);
  }
}
