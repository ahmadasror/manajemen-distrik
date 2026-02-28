package com.template.usermanagement.user.dto;

import com.template.usermanagement.user.Role;
import com.template.usermanagement.user.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private long userCount;
    private List<RoleUserSummary> users;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class RoleUserSummary {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private Boolean isActive;

        public static RoleUserSummary from(User user) {
            return RoleUserSummary.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .isActive(user.getIsActive())
                    .build();
        }
    }

    public static RoleResponse fromList(Role role, long userCount) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .userCount(userCount)
                .users(null)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    public static RoleResponse fromDetail(Role role, List<User> users) {
        List<RoleUserSummary> summaries = users.stream()
                .map(RoleUserSummary::from)
                .collect(Collectors.toList());
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .userCount(summaries.size())
                .users(summaries)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
