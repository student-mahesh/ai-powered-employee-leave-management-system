package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "chat_conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public ChatConversation(AppUser user, String title) { this.user = user; this.title = title; }
}
