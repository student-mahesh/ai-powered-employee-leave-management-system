package com.mahesh.empleave.ai;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ChatConversation conversation;
    @Column(nullable = false, length = 20) private String role;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
    @Lob private String citations;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage(ChatConversation conversation, String role, String content, String citations) {
        this.conversation = conversation; this.role = role; this.content = content; this.citations = citations;
    }
}
