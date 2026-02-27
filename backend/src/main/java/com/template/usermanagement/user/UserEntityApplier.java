package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.user.dto.UserResponse;
import com.template.usermanagement.workflow.EntityApplier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserEntityApplier implements EntityApplier {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserEntityApplier(@Lazy UserService userService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getEntityType() {
        return "USER";
    }

    @Override
    public Long applyCreate(Map<String, Object> payload, String performedBy) {
        User user = userService.applyCreate(payload, performedBy);
        return user.getId();
    }

    @Override
    public void applyUpdate(Long entityId, Map<String, Object> payload, String performedBy) {
        userService.applyUpdate(entityId, payload, performedBy);
    }

    @Override
    public void applyDelete(Long entityId, String performedBy) {
        userService.applyDelete(entityId, performedBy);
    }

    @Override
    public Map<String, Object> getCurrentState(Long entityId) {
        return userRepository.findByIdAndDeletedFalse(entityId)
                .map(user -> objectMapper.convertValue(UserResponse.from(user), Map.class))
                .orElse(null);
    }
}
