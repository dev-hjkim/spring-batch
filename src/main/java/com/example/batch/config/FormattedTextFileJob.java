package com.example.batch.config;

import com.example.batch.domain.CustomerWithEmail;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.batch.item.mail.builder.SimpleMailMessageItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import javax.sql.DataSource;


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
    public FlatFileItemReader<CustomerWithEmail> customerEmailFileReader() {
        Resource inputFile = new ClassPathResource("input/customerWithEmail.csv");

        return new FlatFileItemReaderBuilder<CustomerWithEmail>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip",
                        "email"})
                .targetType(CustomerWithEmail.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CustomerWithEmail> customerBatchWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerWithEmail>()
                .namedParametersJdbcTemplate(new NamedParameterJdbcTemplate(dataSource))
                .sql("INSERT INTO CUSTOMER_WITH_EMAIL (first_name, middle_initial, last_name, " +
                        "address, city, state, zip, email) " +
                        "VALUES(:firstName, :middleInitial, :lastName," +
                        ":address, :city, :state, :zip, :email)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcCursorItemReader<CustomerWithEmail> customerCursorItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<CustomerWithEmail>()
                .name("customerItemReader")
                .dataSource(dataSource)
                .sql("select * from customer_with_email")
                .rowMapper(new BeanPropertyRowMapper<>(CustomerWithEmail.class))
                .build();
    }

    @Bean
    public SimpleMailMessageItemWriter emailItemWriter(MailSender mailSender) {
        return new SimpleMailMessageItemWriterBuilder()
                .mailSender(mailSender)
                .build();
    }

    @Bean
    public Step importStep() throws Exception {
        return this.stepBuilderFactory.get("importStep")
                .<CustomerWithEmail, CustomerWithEmail>chunk(10)
                .reader(customerEmailFileReader())
                .writer(customerBatchWriter(null))
                .build();
    }

    @Bean
    public Step emailStep() throws Exception {
        return this.stepBuilderFactory.get("emailStep")
                .<CustomerWithEmail, SimpleMailMessage>chunk(10)
                .reader(customerCursorItemReader(null))
                .processor((ItemProcessor<CustomerWithEmail, SimpleMailMessage>) customer -> {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setFrom("prospringbatch@gmail.com");
                    mail.setTo(customer.getEmail());
                    mail.setSubject("Welcome!");
                    mail.setText(String.format("Welcome %s %s,\nYou were " +
                            "imported into the system using Spring Batch!",
                            customer.getFirstName(), customer.getLastName()));
                    return mail;
                } )
                .writer(emailItemWriter(null))
                .build();
    }

    @Bean
    public Job emailJob() throws Exception {
        return this.jobBuilderFactory.get("emailJob")
                .start(importStep())
                .next(emailStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
