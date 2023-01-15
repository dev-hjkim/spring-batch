package com.example.batch.skipper;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ParseException;

import java.io.FileNotFoundException;

public class FileVerificationSkipper implements SkipPolicy {
    public boolean shouldSkip(Throwable exception, int skipCount)
        throws SkipLimitExceededException {
        if (exception instanceof FileNotFoundException) {
            return false;
        } else if (exception instanceof ParseException && skipCount <= 10) {
            return true;
        } else {
            return false;
        }
    }
}
