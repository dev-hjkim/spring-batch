package com.example.batch.config;

import com.example.batch.domain.MongoCustomer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;

import javax.persistence.EntityManagerFactory;

@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<MongoCustomer> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<MongoCustomer>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(MongoCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public MongoItemWriter<MongoCustomer> mongoItemWriter(MongoOperations mongoTemplate) {
        return new MongoItemWriterBuilder<MongoCustomer>()
                .collection("customers")
                .template(mongoTemplate)
                .build();
    }

    @Bean
    public Step mongoFormatStep() throws Exception {
        return this.stepBuilderFactory.get("jpaFormatStep")
                .<MongoCustomer, MongoCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(mongoItemWriter(null))
                .build();
    }

    @Bean
    public Job mongoFormatJob() throws Exception {
        return this.jobBuilderFactory.get("mongoFormatJob")
                .start(mongoFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
