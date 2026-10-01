package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ParkingSession parkingSession;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_time", nullable = false)
    @Builder.Default
    private LocalDateTime paymentTime = LocalDateTime.now();

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // CASH, CARD, UPI, MOBILE_WALLET

    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING"; // PENDING, PAID, FAILED, REFUNDED
}
