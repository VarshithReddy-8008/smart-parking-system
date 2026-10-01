package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 50)
    private String ruleName;

    @Column(name = "factor_type", nullable = false, length = 30)
    private String factorType;

    @Column(name = "target_value", nullable = false, length = 50)
    private String targetValue;

    @Column(name = "multiplier", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.ONE;

    @Column(name = "flat_surcharge", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal flatSurcharge = BigDecimal.ZERO;

    @Column(name = "description", length = 255)
    private String description;
}
