-- El admin seedeado también puede enviar propuestas para poder probar la plataforma
INSERT INTO user_tenant_roles (user_id, tenant_id, role)
SELECT
    'a0000000-0000-0000-0000-000000000001',
    t.id,
    'SPEAKER'
FROM tenants t
WHERE t.slug = 'jugbaq'
ON CONFLICT DO NOTHING;