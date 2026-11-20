package com.schoolos.persistence.audit;

import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.core.application.audit.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/** The shared append-only audit log (spec 0005, plan R1): insert only. */
@Repository
class JpaAuditLog implements AuditLog {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void append(AuditEntry entry) {
        em.persist(new AuditEntryEntity(entry));
    }
}
