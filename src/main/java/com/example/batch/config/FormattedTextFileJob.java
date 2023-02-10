package com.example.batch.config;

import com.example.batch.domain.HibernateCustomer;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.database.builder.HibernateItemWriterBuilder;
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
    public FlatFileItemReader<HibernateCustomer> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<HibernateCustomer>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(HibernateCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public HibernateItemWriter<HibernateCustomer> hibernateItemWriter(EntityManagerFactory entityManager) {
        return new HibernateItemWriterBuilder<HibernateCustomer>()
                .sessionFactory(entityManager.unwrap(SessionFactory.class))
                .build();
    }

    @Bean
    public Step hibernateFormatStep() throws Exception {
        return this.stepBuilderFactory.get("hibernateFormatStep")
                .<HibernateCustomer, HibernateCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(hibernateItemWriter(entityManager))
                .build();
    }

    @Bean
    public Job hibernateFormatJob() throws Exception {
        return this.jobBuilderFactory.get("hibernateFormatJob")
                .start(hibernateFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
