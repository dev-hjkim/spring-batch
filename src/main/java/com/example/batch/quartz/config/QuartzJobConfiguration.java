//package com.example.batch.quartz.config;
//
//import com.example.batch.quartz.job.BatchScheduledJob;
//import org.quartz.*;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
//import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
//import org.springframework.batch.core.launch.support.RunIdIncrementer;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class QuartzJobConfiguration {
//    @Autowired
//    private JobBuilderFactory jobBuilderFactory;
//
//    @Autowired
//    private StepBuilderFactory stepBuilderFactory;
//
//    @Bean
//    public Job quartzJob() {
//        return this.jobBuilderFactory.get("quartzJob")
//                .incrementer(new RunIdIncrementer())
//                .start(step1())
//                .build();
//    }
//
//    @Bean
//    public Step step1() {
//        return this.stepBuilderFactory.get("step1")
//                .tasklet((stepContribution, chunkContext) -> {
//                    System.out.println("step1 ran!");
//                    return RepeatStatus.FINISHED;
//                }).build();
//    }
//
//    @Bean
//    public JobDetail quartzJobDetail() {
//        return JobBuilder.newJob(BatchScheduledJob.class)
//                .storeDurably()
//                .build();
//    }
//
//    @Bean
//    public Trigger jobTrigger() {
//        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
//                .withIntervalInSeconds(5).withRepeatCount(4);
//
//        return TriggerBuilder.newTrigger()
//                .forJob(quartzJobDetail())
//                .withSchedule(scheduleBuilder)
//                .build();
//    }
//}
