package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.UserRepository;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {
    private final UserRepository users;
    private final PolicyDocumentRepository documents;
    private final PolicyIngestionService ingestion;
    private final AiChatService chat;
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;

    public record ChatRequest(Long conversationId, @NotBlank @Size(max = 4000) String message) { }
    public record DocumentResponse(Long id, String fileName, String version) { }
    public record PolicyDocumentResponse(Long id, String fileName, String version, LocalDateTime createdAt, Long uploadedById) { }
    public record ConversationResponse(Long id, String title) { }
    public record MessageResponse(Long id, String role, String content, String citations) { }

    @PostMapping("/policies")
    @PreAuthorize("hasRole('ADMIN')")
    DocumentResponse upload(Authentication authentication, @RequestPart("file") MultipartFile file, @RequestParam(required = false) String version) {
        var document = ingestion.ingest(file, version, users.findByEmail(authentication.getName()).orElseThrow());
        return new DocumentResponse(document.getId(), document.getFileName(), document.getVersion());
    }

    @GetMapping("/policies")
    @PreAuthorize("hasRole('ADMIN')")
    List<PolicyDocumentResponse> policies() {
        return documents.findAllByOrderByCreatedAtDesc().stream().map(this::documentResponse).toList();
    }

    @PostMapping("/policies/{id}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    ReindexResponse reindex(@PathVariable Long id) {
        var document = documents.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Policy document not found"));
        return new ReindexResponse(document.getId(), ingestion.reindex(document));
    }

    public record ReindexResponse(Long documentId, int indexedChunks) { }

    @PostMapping("/chat")
    AiChatService.ChatReply ask(Authentication authentication, @RequestBody @jakarta.validation.Valid ChatRequest request) {
        var started = System.nanoTime();
        try {
            return chat.ask(users.findByEmail(authentication.getName()).orElseThrow(), request.conversationId(), request.message());
        } finally {
            log.info("ai_chat_request durationMs={}", elapsedMs(started));
        }
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private PolicyDocumentResponse documentResponse(PolicyDocument document) {
        return new PolicyDocumentResponse(document.getId(), document.getFileName(), document.getVersion(), document.getCreatedAt(), document.getUploadedBy().getId());
    }

    @GetMapping("/conversations")
    List<ConversationResponse> conversations(Authentication authentication) {
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        return conversations.findByUserOrderByCreatedAtDesc(user).stream().map(item -> new ConversationResponse(item.getId(), item.getTitle())).toList();
    }

    @GetMapping("/conversations/{id}/messages")
    List<MessageResponse> messages(Authentication authentication, @PathVariable Long id) {
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        var conversation = conversations.findByIdAndUser(id, user).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        return messages.findByConversationOrderByCreatedAtAsc(conversation).stream().map(item -> new MessageResponse(item.getId(), item.getRole(), item.getContent(), item.getCitations())).toList();
    }
}
