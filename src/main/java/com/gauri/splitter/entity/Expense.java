package com.gauri.splitter.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version // optimistic locking: protects against two people editing at once
    private Long version;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}