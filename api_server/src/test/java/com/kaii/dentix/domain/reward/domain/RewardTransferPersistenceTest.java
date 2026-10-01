package com.kaii.dentix.domain.reward.domain;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.LockMode;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class RewardTransferPersistenceTest {
    SessionFactory factory() {
        return new Configuration().addAnnotatedClass(UserRewardTransaction.class).addAnnotatedClass(OralExerciseContent.class)
                .setProperty("hibernate.connection.driver_class","org.h2.Driver")
                .setProperty("hibernate.connection.url","jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto","create-drop").buildSessionFactory();
    }
    Long insert(SessionFactory sf) {
        try(var session=sf.openSession()) {
            var tx=session.beginTransaction();
            var reward=UserRewardTransaction.builder().userId(1L).type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                    .status(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING).idempotencyKey("one-reward")
                    .coinId("essential_video_1").amount(1).transferRecipientAddress("wallet").tokenContractAddress("contract").build();
            session.persist(reward);tx.commit();return reward.getUserRewardTransactionId();
        }
    }
    @Test void committedCheckpointSurvivesSessionRestartAndExcludesRewardCount() {
        try(var sf=factory()) {
            Long id=insert(sf);
            try(var session=sf.openSession()) {
                var tx=session.beginTransaction();var reward=session.find(UserRewardTransaction.class,id);
                reward.claimTransfer(new Date());tx.commit();
            }
            try(var session=sf.openSession()) {
                var reward=session.find(UserRewardTransaction.class,id);
                assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING);
                assertThat(reward.transferAttempts()).isEqualTo(1);
                assertThat(reward.getNextTransferCheckAt()).isNotNull();
                assertThat(reward.isRewardReceived()).isFalse();
            }
        }
    }
    @Test void competingDatabaseClaimsDispatchOnlyOnce() throws Exception {
        try(var sf=factory()) {
            Long id=insert(sf);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
            var dispatched=new AtomicInteger();var pool=Executors.newFixedThreadPool(2);
            Callable<Void> claim=()->{
                try(var session=sf.openSession()) {
                    ready.countDown();start.await(5,TimeUnit.SECONDS);var tx=session.beginTransaction();
                    var reward=session.get(UserRewardTransaction.class,id,LockMode.PESSIMISTIC_WRITE);
                    if(reward.getStatus()==UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING) {
                        reward.claimTransfer(new Date());dispatched.incrementAndGet();
                    }
                    tx.commit();return null;
                }
            };
            try {var a=pool.submit(claim);var b=pool.submit(claim);assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();
                start.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);assertThat(dispatched).hasValue(1);
            } finally {pool.shutdownNow();}
        }
    }
}
