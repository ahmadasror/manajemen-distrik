-- Password: admin123 (BCrypt encoded)
INSERT INTO users (username, email, password_hash, full_name, is_active, created_by, updated_by)
VALUES ('admin', 'admin@template.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Administrator', true, 'system', 'system');

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';
