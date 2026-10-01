package com.smartparking.repository;

import com.smartparking.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
    List<AiRecommendation> findTop5ByOrderByRecommendedAtDesc();
}
