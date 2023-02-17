package com.example.batch.config;

import com.example.batch.domain.JpaCustomer;
import com.example.batch.repository.JpaCustomerRepository;
import com.example.batch.service.LoggingService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableBatchProcessing
@Configuration
@EnableJpaRepositories(basePackageClasses = JpaCustomerRepository.class)
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
    public ItemWriterAdapter<JpaCustomer> itemWriter(LoggingService loggingService) {
        ItemWriterAdapter<JpaCustomer> customerItemWriterAdapter = new ItemWriterAdapter<>();

        customerItemWriterAdapter.setTargetObject(loggingService);
        customerItemWriterAdapter.setTargetMethod("logCustomer");

        return customerItemWriterAdapter;
    }

    @Bean
    public Step formatStep() throws Exception {
        return this.stepBuilderFactory.get("formatStep")
                .<JpaCustomer, JpaCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(itemWriter(null))
                .build();
    }

    @Bean
    public Job itemWriterAdapterFormatJob() throws Exception {
        return this.jobBuilderFactory.get("itemWriterAdapterFormatJob")
                .start(formatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
