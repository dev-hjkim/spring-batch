package com.example.batch.config;

import com.example.batch.domain.JpaCustomer;
import com.example.batch.repository.JpaCustomerRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
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
    private JpaCustomerRepository jpaCustomerRepository;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory,
                                JpaCustomerRepository jpaCustomerRepository) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jpaCustomerRepository = jpaCustomerRepository;
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
    public RepositoryItemWriter<JpaCustomer> repositoryItemWriter(JpaCustomerRepository repository) {
        return new RepositoryItemWriterBuilder<JpaCustomer>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step repositoryFormatStep() throws Exception {
        return this.stepBuilderFactory.get("repositoryFormatStep")
                .<JpaCustomer, JpaCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(repositoryItemWriter(jpaCustomerRepository))
                .build();
    }

    @Bean
    public Job repositoryFormatJob() throws Exception {
        return this.jobBuilderFactory.get("repositoryFormatJob")
                .start(repositoryFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
