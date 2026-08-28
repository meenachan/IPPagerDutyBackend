package com.IpPagerDuty.ipDeadlineTracker;

import com.IpPagerDuty.ipDeadlineTracker.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class IpDeadlineTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IpDeadlineTrackerApplication.class, args);
	}

}
