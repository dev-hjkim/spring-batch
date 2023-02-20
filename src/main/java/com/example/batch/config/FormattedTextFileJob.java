package com.example.batch.config;

import com.example.batch.domain.CustomerWithEmail;
import com.example.batch.suffixCreator.CustomerOutputFileSuffixCreator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private CustomerOutputFileSuffixCreator customerOutputFileSuffixCreator;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory,
                                CustomerOutputFileSuffixCreator customerOutputFileSuffixCreator) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.customerOutputFileSuffixCreator = customerOutputFileSuffixCreator;
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
    public StaxEventItemWriter<CustomerWithEmail> delegateItemWriter() throws Exception {
        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", CustomerWithEmail.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        marshaller.afterPropertiesSet();

        return new StaxEventItemWriterBuilder<CustomerWithEmail>()
                .name("customerItemWriter")
                .marshaller(marshaller)
                .rootTagName("customers")
                .build();
    }

    @Bean
    public MultiResourceItemWriter<CustomerWithEmail> multiCustomerFileWriter(
            CustomerOutputFileSuffixCreator suffixCreator
    ) throws Exception {
        return new MultiResourceItemWriterBuilder<CustomerWithEmail>()
                .name("multiCustomerFileWriter")
                .delegate(delegateItemWriter())
                .itemCountLimitPerResource(25)
                .resource(new FileSystemResource("output/multiResourceCustomer"))
                .resourceSuffixCreator(suffixCreator)
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
    public Step multiXmlGeneratorStep() throws Exception {
        return this.stepBuilderFactory.get("multiXmlGeneratorStep")
                .<CustomerWithEmail, CustomerWithEmail>chunk(10)
                .reader(customerCursorItemReader(null))
                .writer(multiCustomerFileWriter(customerOutputFileSuffixCreator))
                .build();
    }

    @Bean
    public Job xmlGeneratorJob() throws Exception {
        return this.jobBuilderFactory.get("xmlGeneratorJob")
                .start(importStep())
                .next(multiXmlGeneratorStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
