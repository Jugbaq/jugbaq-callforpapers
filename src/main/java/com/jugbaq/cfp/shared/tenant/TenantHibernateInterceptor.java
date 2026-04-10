package com.jugbaq.cfp.shared.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantHibernateInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantHibernateInterceptor.class);

    private final EntityManager entityManager;

    public TenantHibernateInterceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* com.jugbaq.cfp..*.find*(..)) || " +
            "execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public void enableTenantFilter() {
        TenantContext.getTenantId().ifPresent(tenantId -> {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter")
                        .setParameter("tenantId", tenantId);
                log.trace("Hibernate tenant filter enabled for tenant: {}", tenantId);
            }
        });
    }
}
