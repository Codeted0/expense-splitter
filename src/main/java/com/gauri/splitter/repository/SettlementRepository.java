package com.gauri.splitter.repository;

import com.gauri.splitter.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s join fetch s.fromUser join fetch s.toUser " +
            "where s.group.id = :groupId order by s.createdAt desc, s.id desc")
    List<Settlement> findByGroupIdWithUsers(Long groupId);
}