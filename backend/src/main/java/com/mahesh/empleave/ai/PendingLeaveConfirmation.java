package com.mahesh.empleave.ai;

import com.mahesh.empleave.leave.LeaveApplication;
import com.mahesh.empleave.user.AppUser;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "pending_leave_confirmations", uniqueConstraints = @UniqueConstraint(name = "uk_pending_leave_confirmation_conversation_user", columnNames = {"conversation_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PendingLeaveConfirmation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private ChatConversation conversation;
    @ManyToOne(optional = false) private AppUser user;
    @Column(nullable = false, length = 80) private String leaveType;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false, length = 500) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.WAITING;
    @OneToOne private LeaveApplication application;
    @Version private Long version;

    PendingLeaveConfirmation(ChatConversation conversation, AppUser user, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.conversation = conversation; this.user = user; this.leaveType = leaveType; this.startDate = startDate; this.endDate = endDate; this.reason = reason;
    }
    void replace(String leaveType, LocalDate startDate, LocalDate endDate, String reason) { this.leaveType = leaveType; this.startDate = startDate; this.endDate = endDate; this.reason = reason; this.application = null; this.status = Status.WAITING; }
    void cancel() { status = Status.CANCELLED; }
    void submitted(LeaveApplication application) { this.application = application; this.status = Status.SUBMITTED; }
    enum Status { WAITING, CANCELLED, SUBMITTED }
}
