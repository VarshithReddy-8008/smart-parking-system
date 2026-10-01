package com.smartparking.repository;

import com.smartparking.model.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByFactorType(String factorType);
}
