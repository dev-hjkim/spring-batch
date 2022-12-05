package com.example.batch;

import com.example.batch.step.tasklet.ExploringTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobExplorer jobExplorer;

    @Bean
    public Tasklet explorerTasklet() {
        return new ExploringTasklet(this.jobExplorer);
    }

    @Bean
    public Step explorerStep() {
        return this.stepBuilderFactory.get("explorerStep")
                .tasklet(explorerTasklet())
                .build();
    }

    @Bean
    public Job explorerJob() {
        return this.jobBuilderFactory.get("explorerJob")
                .start(explorerStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BatchApplication.class);

        Properties properties = new Properties();
        properties.put("spring.batch.job.enabled", false);
        application.setDefaultProperties(properties);

        application.run(args);
    }
}
