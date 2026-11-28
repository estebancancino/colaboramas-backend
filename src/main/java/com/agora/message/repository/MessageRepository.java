package com.agora.message.repository;

import com.agora.message.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByToUserIdOrderByCreatedAtDesc(Long toUserId);

    List<Message> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId);

    boolean existsByFromUserIdAndToUserIdAndStatusIn(Long fromUserId, Long toUserId, Collection<String> statuses);

    long countByFromUserIdAndCreatedAtAfter(Long fromUserId, Instant since);
}
