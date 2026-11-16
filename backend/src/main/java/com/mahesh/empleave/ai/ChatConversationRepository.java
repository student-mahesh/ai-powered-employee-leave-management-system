package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.AppUser;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByUserOrderByCreatedAtDesc(AppUser user);
    Optional<ChatConversation> findByIdAndUser(Long id, AppUser user);
}
