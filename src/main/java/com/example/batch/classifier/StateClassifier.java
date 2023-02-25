package com.example.batch.classifier;

import com.example.batch.domain.CustomerWithEmail;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.classify.Classifier;

public class StateClassifier implements Classifier<CustomerWithEmail, ItemWriter<? super CustomerWithEmail>> {

    private ItemWriter<CustomerWithEmail> fileItemWriter;
    private ItemWriter<CustomerWithEmail> jdbcItemWriter;

    public StateClassifier(StaxEventItemWriter<CustomerWithEmail> fileItemWriter,
                           JdbcBatchItemWriter<CustomerWithEmail> jdbcItemWriter) {
        this.fileItemWriter = fileItemWriter;
        this.jdbcItemWriter = jdbcItemWriter;
    }

    @Override
    public ItemWriter<CustomerWithEmail> classify(CustomerWithEmail customer) {
        if (customer.getState().matches("^[A-M].*")) {
            return fileItemWriter;
        } else {
            return jdbcItemWriter;
        }
    }
}
