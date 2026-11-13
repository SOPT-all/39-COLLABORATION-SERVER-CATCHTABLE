package org.sopt.catchtable.vote.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.sopt.catchtable.vote.domain.Vote;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VoteRepositoryImpl implements VoteRepositoryCustom {

    private static final int LOCK_WAIT_TIMEOUT_SECONDS = 3;

    private final EntityManager entityManager;

    @Override
    public Optional<Vote> findByIdForUpdate(Long voteId) {
        Session session = entityManager.unwrap(Session.class);
        int originalLockWaitTimeout = getLockWaitTimeout(session);
        setLockWaitTimeout(session, LOCK_WAIT_TIMEOUT_SECONDS);

        try {
            return entityManager.createQuery("""
                    select vote
                    from Vote vote
                    where vote.id = :voteId
                    """, Vote.class)
                .setParameter("voteId", voteId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
        } finally {
            setLockWaitTimeout(session, originalLockWaitTimeout);
        }
    }

    private int getLockWaitTimeout(Session session) {
        return session.doReturningWork(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT @@SESSION.innodb_lock_wait_timeout")) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        });
    }

    private void setLockWaitTimeout(Session session, int lockWaitTimeout) {
        session.doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SET SESSION innodb_lock_wait_timeout = ?")) {
                statement.setInt(1, lockWaitTimeout);
                statement.execute();
            }
        });
    }
}
