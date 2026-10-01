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
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "response_text", nullable = false, columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
