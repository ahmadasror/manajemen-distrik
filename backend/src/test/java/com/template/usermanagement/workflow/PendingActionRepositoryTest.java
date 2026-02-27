package com.template.usermanagement.workflow;

import com.template.usermanagement.user.Role;
import com.template.usermanagement.user.RoleRepository;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PendingActionRepositoryTest {

    @Autowired
    private PendingActionRepository pendingActionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User maker;
    private User checker;

    @BeforeEach
    void setUp() {
        Role makerRole = roleRepository.findByName("MAKER").orElseThrow();
        Role checkerRole = roleRepository.findByName("CHECKER").orElseThrow();

        maker = User.builder()
                .username("maker1")
                .email("maker1@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("Maker One")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(makerRole))
                .build();
        maker.setCreatedBy("system");
        maker.setUpdatedBy("system");
        maker = userRepository.save(maker);

        checker = User.builder()
                .username("checker1")
                .email("checker1@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("Checker One")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(checkerRole))
                .build();
        checker.setCreatedBy("system");
        checker.setUpdatedBy("system");
        checker = userRepository.save(checker);
    }

    private PendingAction createAction(String entityType, Long entityId, String actionType, String status) {
        PendingAction pa = PendingAction.builder()
                .entityType(entityType)
                .entityId(entityId)
                .actionType(actionType)
                .payload(Map.of("key", "value"))
                .status(status)
                .maker(maker)
                .build();
        return pendingActionRepository.save(pa);
    }

    @Test
    void findAllFiltered_shouldReturnAll() {
        createAction("USER", null, "CREATE", "PENDING");
        createAction("USER", 1L, "UPDATE", "APPROVED");

        Page<PendingAction> result = pendingActionRepository.findAllFiltered(null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAllFiltered_shouldFilterByStatus() {
        createAction("USER", null, "CREATE", "PENDING");
        createAction("USER", 1L, "UPDATE", "APPROVED");

        Page<PendingAction> result = pendingActionRepository.findAllFiltered("PENDING", null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void findAllFiltered_shouldFilterByEntityType() {
        createAction("USER", null, "CREATE", "PENDING");

        Page<PendingAction> result = pendingActionRepository.findAllFiltered(null, "USER", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllFiltered_shouldFilterByBothStatusAndEntityType() {
        createAction("USER", null, "CREATE", "PENDING");
        createAction("USER", 1L, "UPDATE", "APPROVED");

        Page<PendingAction> result = pendingActionRepository.findAllFiltered("PENDING", "USER", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByEntityTypeAndEntityIdAndStatus_shouldFindMatch() {
        createAction("USER", 5L, "UPDATE", "PENDING");

        Optional<PendingAction> result = pendingActionRepository.findByEntityTypeAndEntityIdAndStatus("USER", 5L, "PENDING");
        assertThat(result).isPresent();
    }

    @Test
    void findByEntityTypeAndEntityIdAndStatus_shouldReturnEmptyForNoMatch() {
        Optional<PendingAction> result = pendingActionRepository.findByEntityTypeAndEntityIdAndStatus("USER", 999L, "PENDING");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByEntityTypeAndEntityIdAndStatus_shouldReturnTrue() {
        createAction("USER", 10L, "DELETE", "PENDING");

        assertThat(pendingActionRepository.existsByEntityTypeAndEntityIdAndStatus("USER", 10L, "PENDING")).isTrue();
    }

    @Test
    void existsByEntityTypeAndEntityIdAndStatus_shouldReturnFalse() {
        assertThat(pendingActionRepository.existsByEntityTypeAndEntityIdAndStatus("USER", 999L, "PENDING")).isFalse();
    }

    @Test
    void countByStatus_shouldCountCorrectly() {
        createAction("USER", null, "CREATE", "PENDING");
        createAction("USER", 1L, "UPDATE", "PENDING");
        createAction("USER", 2L, "DELETE", "APPROVED");

        assertThat(pendingActionRepository.countByStatus("PENDING")).isEqualTo(2);
        assertThat(pendingActionRepository.countByStatus("APPROVED")).isEqualTo(1);
        assertThat(pendingActionRepository.countByStatus("REJECTED")).isEqualTo(0);
    }

    @Test
    void findByMakerId_shouldReturnMakerActions() {
        createAction("USER", null, "CREATE", "PENDING");

        Page<PendingAction> result = pendingActionRepository.findByMakerId(maker.getId(), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByMakerId_shouldReturnEmptyForOtherMaker() {
        createAction("USER", null, "CREATE", "PENDING");

        Page<PendingAction> result = pendingActionRepository.findByMakerId(checker.getId(), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
