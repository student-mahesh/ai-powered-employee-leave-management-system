package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.AppUser;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

interface PendingLeaveConfirmationRepository extends JpaRepository<PendingLeaveConfirmation, Long> {
    Optional<PendingLeaveConfirmation> findByConversationAndUser(ChatConversation conversation, AppUser user);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingLeaveConfirmation p where p.conversation = ?1 and p.user = ?2")
    Optional<PendingLeaveConfirmation> findForUpdate(ChatConversation conversation, AppUser user);
}
