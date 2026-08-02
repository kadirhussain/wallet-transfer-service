package com.wallet.transfer.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferMetrics {

    private final MeterRegistry registry;
    private Counter successCounter, failureCounter, duplicateCounter;
    private DistributionSummary amountSummary;
    private Timer processingTimer;

    @PostConstruct
    public void init() {

        successCounter   = Counter.builder("transfers.processed.total").description("Successfully processed transfers").register(registry);

        failureCounter   = Counter.builder("transfers.failed.total").description("Failed transfers").register(registry);

        duplicateCounter = Counter.builder("transfers.duplicate.total").description("Duplicate idempotency key hits").register(registry);

        amountSummary    = DistributionSummary.builder("transfers.amount").description("Transfer amounts").baseUnit("INR").publishPercentiles(0.5,0.95,0.99).register(registry);

        processingTimer  = Timer.builder("transfer.processing.duration").description("Transfer processing duration").publishPercentiles(0.5,0.95,0.99).register(registry);

    }

    public void recordSuccess(BigDecimal amount) {
        successCounter.increment();
        amountSummary.record(amount.doubleValue());
    }

    public void recordFailure() {
        failureCounter.increment();
    }
    public void recordDuplicate() {
        duplicateCounter.increment();
    }
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }
    public void stopTimer(Timer.Sample s) {
        s.stop(processingTimer);
    }



}
