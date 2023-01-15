package com.example.batch;

import com.example.batch.domain.Customer;
import com.example.batch.itemreader.CustomerItemReader;
import com.example.batch.listener.CustomerItemListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;


    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(copyFileStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(10)
                .reader(customerItemReader())
                .writer(itemWriter())
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .listener(customerListener())
                .build();
    }


    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public CustomerItemReader customerItemReader() {
        CustomerItemReader customerItemReader = new CustomerItemReader();

        customerItemReader.setName("customerItemReader");

        return customerItemReader;
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public CustomerItemListener customerListener() {
        return new CustomerItemListener();
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
