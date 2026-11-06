package com.observability.monitor.service.payment;

import com.observability.monitor.config.MonitorProperties;
import com.observability.monitor.exception.BusinessRuleException;
import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.service.payment.Payment.PaymentStatus;
import com.observability.monitor.service.support.AbstractManagedService;
import com.observability.monitor.service.support.LatencySimulator;
import com.observability.monitor.service.support.OperationParameters;
import com.observability.monitor.service.support.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Processes and refunds payments. A configurable share of calls (10% by default) fails on purpose to
 * emulate an unreliable external payment gateway.
 */
@Service
public class PaymentService extends AbstractManagedService {

    public static final String SERVICE_ID = "payments";

    public static final String PROCESS_PAYMENT = "process-payment";
    public static final String GET_PAYMENT = "get-payment";
    public static final String LIST_PAYMENTS = "list-payments";
    public static final String REFUND_PAYMENT = "refund-payment";

    private static final String DEFAULT_CURRENCY = "USD";

    private final ConcurrentMap<String, Payment> payments = new ConcurrentHashMap<>();
    private final FailureInjector failureInjector;
    private final Clock clock;

    @Autowired
    public PaymentService(MonitorProperties properties) {
        this(new LatencySimulator(80, 320), new FailureInjector(properties.payments().failureRate()), Clock.systemUTC());
    }

    public PaymentService(LatencySimulator latency, FailureInjector failureInjector, Clock clock) {
        super(latency);
        this.failureInjector = failureInjector;
        this.clock = clock;

        register(PROCESS_PAYMENT, this::processPayment);
        register(GET_PAYMENT, this::getPayment);
        register(LIST_PAYMENTS, this::listPayments);
        register(REFUND_PAYMENT, this::refundPayment);
    }

    @Override
    public String serviceId() {
        return SERVICE_ID;
    }

    @Override
    public String displayName() {
        return "Payment Service";
    }

    @Override
    protected void beforeOperation(String operation) {
        failureInjector.maybeFail(operation);
    }

    public List<String> paymentIds() {
        return List.copyOf(payments.keySet());
    }

    public List<String> refundablePaymentIds() {
        return payments.values().stream()
                .filter(payment -> payment.status() == PaymentStatus.APPROVED)
                .map(Payment::paymentId)
                .toList();
    }

    public Optional<Payment> findPayment(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }

    private ServiceResponse processPayment(OperationParameters params) {
        String currency = params.optionalText("currency").map(value -> value.toUpperCase(Locale.ROOT)).orElse(DEFAULT_CURRENCY);
        Payment payment = new Payment(
                nextPaymentId(),
                params.requireText("orderId"),
                params.requirePositiveAmount("amount"),
                currency,
                PaymentMethod.parse(params.optionalText("method").orElse(PaymentMethod.CREDIT_CARD.name())),
                PaymentStatus.APPROVED,
                nextAuthorizationCode(),
                clock.instant(),
                null);
        payments.put(payment.paymentId(), payment);
        return ServiceResponse.of("Payment approved", payment);
    }

    private ServiceResponse getPayment(OperationParameters params) {
        return ServiceResponse.of("Payment retrieved", requirePayment(params.requireText("paymentId")));
    }

    private ServiceResponse listPayments(OperationParameters params) {
        Optional<String> orderFilter = params.optionalText("orderId");
        List<Payment> result = payments.values().stream()
                .filter(payment -> orderFilter.map(orderId -> payment.orderId().equals(orderId)).orElse(true))
                .sorted(Comparator.comparing(Payment::processedAt).reversed())
                .toList();
        return ServiceResponse.of("%d payments found".formatted(result.size()), result);
    }

    private ServiceResponse refundPayment(OperationParameters params) {
        String paymentId = params.requireText("paymentId");
        Payment refunded = payments.computeIfPresent(paymentId, (id, current) -> {
            if (current.status() == PaymentStatus.REFUNDED) {
                throw new BusinessRuleException("Payment '%s' has already been refunded".formatted(id));
            }
            return current.refund(clock.instant());
        });
        if (refunded == null) {
            throw new ResourceNotFoundException("Payment", paymentId);
        }
        return ServiceResponse.of("Payment refunded", refunded);
    }

    private Payment requirePayment(String paymentId) {
        return findPayment(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }

    private static String nextPaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String nextAuthorizationCode() {
        return "AUTH-%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
