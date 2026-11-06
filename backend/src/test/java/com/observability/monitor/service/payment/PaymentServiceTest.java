package com.observability.monitor.service.payment;

import com.observability.monitor.exception.BusinessRuleException;
import com.observability.monitor.exception.ServiceUnavailableException;
import com.observability.monitor.service.support.LatencySimulator;
import com.observability.monitor.service.support.ServiceResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PaymentServiceTest {

    @Test
    void approvesAndRefundsPaymentWhenGatewayIsHealthy() {
        PaymentService service = serviceWithFailureRate(0.0);

        ServiceResponse approved = service.execute(PaymentService.PROCESS_PAYMENT,
                Map.of("orderId", "ORD-1", "amount", "120.5", "method", "debit-card"));
        Payment payment = (Payment) approved.data();

        assertThat(payment.status()).isEqualTo(Payment.PaymentStatus.APPROVED);
        assertThat(payment.amount()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(payment.method()).isEqualTo(PaymentMethod.DEBIT_CARD);

        Payment refunded = (Payment) service.execute(PaymentService.REFUND_PAYMENT,
                Map.of("paymentId", payment.paymentId())).data();
        assertThat(refunded.status()).isEqualTo(Payment.PaymentStatus.REFUNDED);

        assertThatThrownBy(() -> service.execute(PaymentService.REFUND_PAYMENT, Map.of("paymentId", payment.paymentId())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void failsEveryCallWhenFailureRateIsOne() {
        PaymentService service = serviceWithFailureRate(1.0);

        assertThatThrownBy(() -> service.execute(PaymentService.LIST_PAYMENTS))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void failsRoughlyTenPercentOfCallsWithDefaultRate() {
        PaymentService service = serviceWithFailureRate(0.10);
        int attempts = 5_000;
        int failures = 0;

        for (int i = 0; i < attempts; i++) {
            try {
                service.execute(PaymentService.LIST_PAYMENTS);
            } catch (ServiceUnavailableException ex) {
                failures++;
            }
        }

        assertThat((double) failures / attempts).isCloseTo(0.10, within(0.025));
    }

    private static PaymentService serviceWithFailureRate(double rate) {
        return new PaymentService(LatencySimulator.none(), new FailureInjector(rate), Clock.systemUTC());
    }
}
