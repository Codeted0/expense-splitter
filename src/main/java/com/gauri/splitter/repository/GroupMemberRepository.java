package com.gauri.splitter.repository;

import com.gauri.splitter.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    // JOIN FETCH loads the user in the same query (avoids the N+1 problem)
    @Query("select m from GroupMember m join fetch m.user where m.group.id = :groupId")
    List<GroupMember> findByGroupIdWithUser(Long groupId);

    @Query("select m from GroupMember m join fetch m.group g join fetch g.createdBy where m.user.id = :userId")
    List<GroupMember> findByUserIdWithGroup(Long userId);
}