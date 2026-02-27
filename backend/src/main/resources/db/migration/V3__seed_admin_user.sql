-- Password: admin123 (BCrypt encoded)
INSERT INTO users (username, email, password_hash, full_name, is_active, created_by)
VALUES ('admin', 'admin@template.com', '$2a$10$9OQ1eSGcZdlOuFHCKtn0mOC9n2zGcrfxLxzO36O1bU4mMpEY4KZsi', 'System Administrator', true, 'system');

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';
