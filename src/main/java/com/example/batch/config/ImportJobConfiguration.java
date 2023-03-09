package com.example.batch.config;

import com.example.batch.aggregator.StatementLineAggregator;
import com.example.batch.callback.StatementHeaderCallback;
import com.example.batch.classifier.CustomerUpdateClassifier;
import com.example.batch.domain.*;
import com.example.batch.itemprocessor.AccountItemProcessor;
import com.example.batch.validator.CustomerItemValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ImportJobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private Step saveStep;

    @Autowired
    private CustomerItemValidator validator;

    @Autowired
    private AccountItemProcessor accountItemProcessor;


//    @Bean
//    public Job job() throws Exception {
//        return this.jobBuilderFactory.get("importJob")
//                .start(saveStep)
//                .next(importCustomerUpdates())
//                .next(importTransactions())
//                .next(applyTransactions())
//                .next(generateStatements(accountItemProcessor))
//                .build();
//    }

    //////////////////////  Step : importCustomerUpdates //////////////////////

    @Bean
    public Step importCustomerUpdates() throws Exception {
        return this.stepBuilderFactory.get("importCustomerUpdates")
                .<CustomerUpdate, CustomerUpdate>chunk(100)
                .reader(customerUpdateItemReader())
                .processor(customerValidatingItemProcessor(validator))
                .writer(customerUpdateItemWriter())
                .build();
    }

    @Bean
    public FlatFileItemReader<CustomerUpdate> customerUpdateItemReader() throws Exception {
        Resource inputFile = new ClassPathResource("input/customer_update.csv");

        return new FlatFileItemReaderBuilder<CustomerUpdate>()
                .name("customerUpdateItemReader")
                .resource(inputFile)
                .lineTokenizer(customerUpdatesLineTokenizer())
                .fieldSetMapper(customerUpdateFieldSetMapper())
                .build();
    }

    @Bean
    public LineTokenizer customerUpdatesLineTokenizer() throws Exception {
        DelimitedLineTokenizer recordType1 = new DelimitedLineTokenizer();
        recordType1.setNames("recordId", "customerId", "firstName",
                "middleName", "lastName");
        recordType1.afterPropertiesSet();

        DelimitedLineTokenizer recordType2 = new DelimitedLineTokenizer();
        recordType2.setNames("recordId", "customerId", "address1",
                "address2", "city", "state", "postalCode");
        recordType2.afterPropertiesSet();

        DelimitedLineTokenizer recordType3 = new DelimitedLineTokenizer();
        recordType3.setNames("recordId", "customerId", "emailAddress",
                "homePhone", "cellPhone", "workPhone", "notificationPreference");
        recordType3.afterPropertiesSet();

        Map<String, LineTokenizer> tokenizers = new HashMap<>(3);
        tokenizers.put("1*", recordType1);
        tokenizers.put("2*", recordType2);
        tokenizers.put("3*", recordType3);

        PatternMatchingCompositeLineTokenizer lineTokenizer =
                new PatternMatchingCompositeLineTokenizer();
        lineTokenizer.setTokenizers(tokenizers);

        return lineTokenizer;
    }

    @Bean
    public FieldSetMapper<CustomerUpdate> customerUpdateFieldSetMapper() {
        return fieldSet -> {
            switch (fieldSet.readInt("recordId")) {
                case 1: return new CustomerNameUpdate(
                        fieldSet.readLong("customerId"),
                        fieldSet.readString("firstName"),
                        fieldSet.readString("middleName"),
                        fieldSet.readString("lastName")
                );
                case 2: return new CustomerAddressUpdate(
                        fieldSet.readLong("customerId"),
                        fieldSet.readString("address1"),
                        fieldSet.readString("address2"),
                        fieldSet.readString("city"),
                        fieldSet.readString("state"),
                        fieldSet.readString("postalCode")
                );
                case 3:
                    String rawPreference = fieldSet.readString("notificationPreference");

                    Integer notificationPreference = null;

                    if (StringUtils.hasText(rawPreference)) {
                        notificationPreference = Integer.parseInt(rawPreference);
                    }

                    return new CustomerContactUpdate(
                            fieldSet.readLong("customerId"),
                            fieldSet.readString("emailAddress"),
                            fieldSet.readString("homePhone"),
                            fieldSet.readString("cellPhone"),
                            fieldSet.readString("workPhone"),
                            notificationPreference
                    );
                default: throw new IllegalArgumentException(
                        "Invalid record type was found:" +
                                fieldSet.readInt("recordId")
                );
            }
        };
    }

    @Bean
    public ValidatingItemProcessor<CustomerUpdate> customerValidatingItemProcessor(
            CustomerItemValidator validator
    ) {
        ValidatingItemProcessor<CustomerUpdate> customerValidatingItemProcessor =
                new ValidatingItemProcessor<>(validator);
        customerValidatingItemProcessor.setFilter(true);
        return customerValidatingItemProcessor;
    }

    @Bean
    public JdbcBatchItemWriter<CustomerUpdate> customerNameUpdateItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerUpdate>()
                .beanMapped()
                .sql("UPDATE APP_CUSTOMER " +
                        "SET FIRST_NAME = COALESCE(:firstName, FIRST_NAME), " +
                        "MIDDLE_NAME = COALESCE(:middleName, MIDDLE_NAME), " +
                        "LAST_NAME = COALESCE(:lastName, LAST_NAME) " +
                        "WHERE CUSTOMER_ID = :customerId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CustomerUpdate> customerAddressUpdateItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerUpdate>()
                .beanMapped()
                .sql("UPDATE APP_CUSTOMER " +
                        "SET ADDRESS1 = COALESCE(:address1, ADDRESS1), " +
                        "ADDRESS2 = COALESCE(:address2, ADDRESS2), " +
                        "CITY = COALESCE(:city, CITY), " +
                        "STATE = COALESCE(:state, STATE), " +
                        "POSTAL_CODE = COALESCE(:postalCode, POSTAL_CODE) " +
                        "WHERE CUSTOMER_ID = :customerId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CustomerUpdate> customerContactUpdateItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerUpdate>()
                .beanMapped()
                .sql("UPDATE APP_CUSTOMER " +
                        "SET EMAIL_ADDRESS = COALESCE(:emailAddress, EMAIL_ADDRESS), " +
                        "HOME_PHONE = COALESCE(:homePhone, HOME_PHONE), " +
                        "CELL_PHONE = COALESCE(:cellPhone, CELL_PHONE), " +
                        "WORK_PHONE = COALESCE(:workPhone, WORK_PHONE), " +
                        "NOTIFICATION_PREF = COALESCE(:notificationPreferences, NOTIFICATION_PREF) " +
                        "WHERE CUSTOMER_ID = :customerId")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public ClassifierCompositeItemWriter<CustomerUpdate> customerUpdateItemWriter() {
        CustomerUpdateClassifier classifier = new CustomerUpdateClassifier(
                customerNameUpdateItemWriter(null),
                customerAddressUpdateItemWriter(null),
                customerContactUpdateItemWriter(null)
        );

        ClassifierCompositeItemWriter<CustomerUpdate> compositeItemWriter =
                new ClassifierCompositeItemWriter<>();

        compositeItemWriter.setClassifier(classifier);

        return compositeItemWriter;
    }


    //////////////////////  Step : importTransactions //////////////////////
    @Bean
    public Step importTransactions() {
        return this.stepBuilderFactory.get("importTransactions")
                .<Transaction2, Transaction2>chunk(100)
                .reader(transactionItemReader())
                .writer(transactionItemWriter(null))
                .build();
    }

    @Bean
    public StaxEventItemReader<Transaction2> transactionItemReader() {
        Resource transactionFile = new ClassPathResource("input/transactions.xml");

        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Transaction2.class);

        return new StaxEventItemReaderBuilder<Transaction2>()
                .name("transactionItemReader")
                .resource(transactionFile)
                .addFragmentRootElements("transaction")
                .unmarshaller(unmarshaller)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction2> transactionItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction2>()
                .dataSource(dataSource)
                .sql("INSERT INTO TRANSACTION2 (TRANSACTION_ID, " +
                        "ACCOUNT_ID, " +
                        "DESCRIPTION, " +
                        "CREDIT, " +
                        "DEBIT, " +
                        "TIMESTAMP) VALUES (:transactionId, " +
                        ":accountId, " +
                        ":description, " +
                        ":credit, " +
                        ":debit, " +
                        ":timestamp)")
                .beanMapped()
                .build();
    }


    //////////////////////  Step : applyTransactions //////////////////////
    @Bean
    public Step applyTransactions() {
        return this.stepBuilderFactory.get("applyTransactions")
                .<Transaction2, Transaction2>chunk(100)
                .reader(applyTransactionReader(null))
                .writer(applyTransactionWriter(null))
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Transaction2> applyTransactionReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Transaction2>()
                .name("applyTransactionReader")
                .dataSource(dataSource)
                .sql("SELECT transaction_id, " +
                        "account_id, " +
                        "description, " +
                        "credit, " +
                        "debit, " +
                        "timestamp " +
                        "from transaction2 " +
                        "order by timestamp")
                .rowMapper((resultSet, i) ->
                        new Transaction2(
                                resultSet.getLong("transaction_id"),
                                resultSet.getLong("account_id"),
                                resultSet.getString("description"),
                                resultSet.getBigDecimal("credit"),
                                resultSet.getBigDecimal("debit"),
                                resultSet.getTimestamp("timestamp")
                        ))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction2> applyTransactionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction2>()
                .dataSource(dataSource)
                .sql("UPDATE ACCOUNT " +
                        "SET BALANCE = BALANCE + :transactionAmount " +
                        "WHERE ID = :accountId")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    //////////////////////  Step : generateStatements //////////////////////
    @Bean
    public Step generateStatements(AccountItemProcessor itemProcessor) {
        return this.stepBuilderFactory.get("generateStatements")
                .<Statement, Statement>chunk(1)
                .reader(statementItemReader(null))
                .processor(itemProcessor)
                .writer(statementItemWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Statement> statementItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Statement>()
                .name("statementItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM APP_CUSTOMER")
                .rowMapper((resultSet, i) -> {
                    AppCustomer customer =
                            new AppCustomer(resultSet.getLong("customer_id"),
                                    resultSet.getString("first_name"),
                                    resultSet.getString("middle_name"),
                                    resultSet.getString("last_name"),
                                    resultSet.getString("address1"),
                                    resultSet.getString("address2"),
                                    resultSet.getString("city"),
                                    resultSet.getString("state"),
                                    resultSet.getString("postal_code"),
                                    resultSet.getString("email_address"),
                                    resultSet.getString("home_phone"),
                                    resultSet.getString("cell_phone"),
                                    resultSet.getString("work_phone"),
                                    resultSet.getString("notification_pref"));
                    return new Statement(customer);
                }).build();
    }

    @Bean
    public FlatFileItemWriter<Statement> individualStatementItemWriter() {
        FlatFileItemWriter<Statement> itemWriter = new FlatFileItemWriter<>();

        itemWriter.setName("individualStatementItemWriter");
        itemWriter.setHeaderCallback(new StatementHeaderCallback());
        itemWriter.setLineAggregator(new StatementLineAggregator());

        return itemWriter;
    }

    @Bean
    public MultiResourceItemWriter<Statement> statementItemWriter() {
        Resource outputDir = new FileSystemResource("output/statement");

        return new MultiResourceItemWriterBuilder<Statement>()
                .name("statementItemWriter")
                .resource(outputDir)
                .itemCountLimitPerResource(1)
                .delegate(individualStatementItemWriter())
                .build();
    }
}
