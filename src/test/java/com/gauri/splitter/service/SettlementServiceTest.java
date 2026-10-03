package com.gauri.splitter.service;

import com.gauri.splitter.dto.CreateSettlementRequest;
import com.gauri.splitter.entity.*;
import com.gauri.splitter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock ExpenseGroupRepository groupRepository;
    @Mock GroupMemberRepository memberRepository;
    @InjectMocks SettlementService service;

    User a, c;
    ExpenseGroup group;
    Settlement pending; // C pays A 400

    @BeforeEach
    void setUp() {
        a = User.builder().id(1L).name("A").email("a@test.com").build();
        c = User.builder().id(3L).name("C").email("c@test.com").build();
        group = ExpenseGroup.builder().id(2L).name("Goa").createdBy(a).build();
        pending = Settlement.builder().id(10L).group(group).fromUser(c).toUser(a)
                .amount(new BigDecimal("400.00")).status(SettlementStatus.PENDING).build();
    }

    private void membersAreAandC() {
        when(memberRepository.findByGroupIdWithUser(2L)).thenReturn(List.of(
                GroupMember.builder().group(group).user(a).role(GroupRole.ADMIN).build(),
                GroupMember.builder().group(group).user(c).role(GroupRole.MEMBER).build()));
    }

    private HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void receiverCanConfirm() {
        membersAreAandC();
        when(settlementRepository.findById(10L)).thenReturn(Optional.of(pending));

        var response = service.confirm("a@test.com", 2L, 10L);

        assertEquals(SettlementStatus.PAID, response.status());
        assertNotNull(response.paidAt());
    }

    @Test
    void payerCannotConfirmOwnPayment() {
        membersAreAandC();
        when(settlementRepository.findById(10L)).thenReturn(Optional.of(pending));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.confirm("c@test.com", 2L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, statusOf(ex));
        assertEquals(SettlementStatus.PENDING, pending.getStatus());
    }

    @Test
    void cannotConfirmTwice() {
        membersAreAandC();
        pending.setStatus(SettlementStatus.PAID);
        when(settlementRepository.findById(10L)).thenReturn(Optional.of(pending));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.confirm("a@test.com", 2L, 10L));

        assertEquals(HttpStatus.CONFLICT, statusOf(ex));
    }

    @Test
    void outsiderIsRejected() {
        membersAreAandC();

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.list("outsider@test.com", 2L));

        assertEquals(HttpStatus.FORBIDDEN, statusOf(ex));
    }

    @Test
    void cannotPayYourself() {
        membersAreAandC();
        when(groupRepository.findById(2L)).thenReturn(Optional.of(group));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.create("a@test.com", 2L,
                        new CreateSettlementRequest(1L, new BigDecimal("50"))));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(ex));
        verify(settlementRepository, never()).save(any());
    }
}