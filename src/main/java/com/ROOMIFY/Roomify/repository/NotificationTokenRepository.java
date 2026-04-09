package com.ROOMIFY.Roomify.repository;

import com.ROOMIFY.Roomify.model.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {

    // Find token by user ID
    Optional<NotificationToken> findByUserId(Long userId);

    // Find all tokens for a list of users (for sending bulk notifications)
    List<NotificationToken> findAllByUserIdIn(List<Long> userIds);

    // Find all active tokens
    @Query("SELECT nt FROM NotificationToken nt WHERE nt.isActive = true")
    List<NotificationToken> findAllActiveTokens();

    // Delete token by user ID
    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationToken nt WHERE nt.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // Delete token by token value
    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationToken nt WHERE nt.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);

    // Check if token exists for user
    boolean existsByUserId(Long userId);

    // Count tokens by user
    @Query("SELECT COUNT(nt) FROM NotificationToken nt WHERE nt.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Find tokens older than a certain date (for cleanup)
    @Query("SELECT nt FROM NotificationToken nt WHERE nt.updatedAt < :date")
    List<NotificationToken> findTokensOlderThan(@Param("date") java.time.LocalDateTime date);
}
