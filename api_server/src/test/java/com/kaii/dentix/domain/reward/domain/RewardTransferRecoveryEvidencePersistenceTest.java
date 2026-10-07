package com.kaii.dentix.domain.reward.domain;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class RewardTransferRecoveryEvidencePersistenceTest {
    SessionFactory factory() {
        return new Configuration().addAnnotatedClass(RewardTransferRecoveryEvidence.class)
                .setProperty("hibernate.connection.driver_class","org.h2.Driver")
                .setProperty("hibernate.connection.url","jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto","create-drop").buildSessionFactory();
    }
    RewardTransferRecoveryEvidence claim(String hash,Long reward) {
        return RewardTransferRecoveryEvidence.builder().factHash(hash).transactionId(reward)
                .adminId(9L).recoveredAt(new Date()).blockHeight(100).build();
    }
    @Test void competingJpaSavesCannotOverwriteReceiptOwner() throws Exception {
        try(var sf=factory()) {
            var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
            var successes=new AtomicInteger();var conflicts=new AtomicInteger();var pool=Executors.newFixedThreadPool(2);
            String hash="a".repeat(64);
            java.util.function.Function<Long,Callable<Void>> writer=id->()->{
                try(var session=sf.openSession()) {
                    var tx=session.beginTransaction();
                    var repository=new SimpleJpaRepository<>(RewardTransferRecoveryEvidence.class,session);
                    assertThat(repository.findById(hash)).isEmpty();
                    ready.countDown();assertThat(start.await(5,TimeUnit.SECONDS)).isTrue();
                    try {repository.saveAndFlush(claim(hash,id));tx.commit();successes.incrementAndGet();}
                    catch(org.hibernate.exception.ConstraintViolationException | jakarta.persistence.EntityExistsException conflict) {
                        tx.rollback();conflicts.incrementAndGet();
                    }
                }
                return null;
            };
            try {
                var first=pool.submit(writer.apply(10L));var second=pool.submit(writer.apply(20L));
                assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();
                first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
                assertThat(successes).hasValue(1);assertThat(conflicts).hasValue(1);
                try(var session=sf.openSession()) {
                    var saved=session.find(RewardTransferRecoveryEvidence.class,hash);
                    assertThat(saved.getTransactionId()).isIn(10L,20L);
                    assertThat(saved.getAdminId()).isEqualTo(9L);assertThat(saved.getRecoveredAt()).isNotNull();
                    assertThat(saved.getBlockHeight()).isEqualTo(100);
                }
            } finally {pool.shutdownNow();}
        }
    }
}
