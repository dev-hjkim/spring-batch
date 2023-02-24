package com.example.batch.config;

import com.example.batch.domain.CustomerWithEmail;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


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
    public FlatFileItemReader<CustomerWithEmail> compositeWriterItemReader() {
        Resource inputFile = new ClassPathResource("input/customerWithEmail.csv");

        return new FlatFileItemReaderBuilder<CustomerWithEmail>()
                .name("compositeWriterItemReader")
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
    public StaxEventItemWriter<CustomerWithEmail> xmlDelegateItemWriter() {
        Resource outputFile = new FileSystemResource("output/compositeXmlCustomer.xml");

        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", CustomerWithEmail.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        marshaller.afterPropertiesSet();

        return new StaxEventItemWriterBuilder<CustomerWithEmail>()
                .name("xmlDelegateItemWriter")
                .resource(outputFile)
                .marshaller(marshaller)
                .rootTagName("customers")
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CustomerWithEmail> jdbcDelegateItemWriter(DataSource dataSource) {
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
    public CompositeItemWriter<CustomerWithEmail> compositeItemWriter() throws Exception {
        return new CompositeItemWriterBuilder<CustomerWithEmail>()
                .delegates(Arrays.asList(xmlDelegateItemWriter(), jdbcDelegateItemWriter(null)))
                .build();
    }

    @Bean
    public Step compositeWriterStep() throws Exception {
        return this.stepBuilderFactory.get("compositeWriterStep")
                .<CustomerWithEmail, CustomerWithEmail>chunk(10)
                .reader(compositeWriterItemReader())
                .writer(compositeItemWriter())
                .build();
    }

    @Bean
    public Job compositeWriterJob() throws Exception {
        return this.jobBuilderFactory.get("compositeWriterJob")
                .start(compositeWriterStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
