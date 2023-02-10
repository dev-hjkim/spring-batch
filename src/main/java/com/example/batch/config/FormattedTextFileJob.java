package com.example.batch.config;

import com.example.batch.domain.JpaCustomer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.persistence.EntityManagerFactory;

@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private EntityManagerFactory entityManager;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory,
                                EntityManagerFactory entityManager) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.entityManager = entityManager;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<JpaCustomer> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<JpaCustomer>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(JpaCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public JpaItemWriter<JpaCustomer> jpaItemWriter(EntityManagerFactory entityManager) {
        JpaItemWriter<JpaCustomer> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManager);
        return jpaItemWriter;
    }

    @Bean
    public Step jpaFormatStep() throws Exception {
        return this.stepBuilderFactory.get("jpaFormatStep")
                .<JpaCustomer, JpaCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(jpaItemWriter(entityManager))
                .build();
    }

    @Bean
    public Job jpaFormatJob() throws Exception {
        return this.jobBuilderFactory.get("jpaFormatJob")
                .start(jpaFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
