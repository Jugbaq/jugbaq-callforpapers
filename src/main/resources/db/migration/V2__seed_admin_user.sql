-- =========================================
-- V2: Seed admin user for JUGBAQ
-- Password: "admin123" → BCrypt hash
-- CAMBIAR EN PRODUCCIÓN
-- =========================================

-- Insertar usuario admin
INSERT INTO users (id, email, password_hash, full_name, email_verified, status)
VALUES (
           'a0000000-0000-0000-0000-000000000001',
           'admin@jugbaq.dev',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'Admin JUGBAQ',
           true,
           'ACTIVE'
       );

-- Asignar rol ADMIN al tenant JUGBAQ
INSERT INTO user_tenant_roles (user_id, tenant_id, role)
SELECT
    'a0000000-0000-0000-0000-000000000001',
    t.id,
    'ADMIN'
FROM tenants t
WHERE t.slug = 'jugbaq';

-- También darle rol ORGANIZER (un admin también organiza)
INSERT INTO user_tenant_roles (user_id, tenant_id, role)
SELECT
    'a0000000-0000-0000-0000-000000000001',
    t.id,
    'ORGANIZER'
FROM tenants t
WHERE t.slug = 'jugbaq';