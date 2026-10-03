package com.gauri.splitter.repository;

import com.gauri.splitter.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // EntityGraph loads payer, splits and split users in one query (no N+1)
    @EntityGraph(attributePaths = {"paidBy", "splits", "splits.user"})
    List<Expense> findByGroupIdOrderByExpenseDateDescIdDesc(Long groupId);

    @EntityGraph(attributePaths = {"paidBy", "splits", "splits.user"})
    Optional<Expense> findWithDetailsById(Long id);
}