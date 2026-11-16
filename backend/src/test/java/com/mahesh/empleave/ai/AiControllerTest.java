package com.mahesh.empleave.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiControllerTest {
    @Test
    void policiesReturnsStoredDocumentMetadataIncludingUploaderAndCreationTime() {
        var documents = mock(PolicyDocumentRepository.class);
        var controller = new AiController(mock(UserRepository.class), documents, mock(PolicyIngestionService.class), mock(AiChatService.class), mock(ChatConversationRepository.class), mock(ChatMessageRepository.class));
        var uploader = new AppUser("admin@example.com", "hash", Role.ADMIN, "Admin");
        var document = new PolicyDocument("leave-policy.pdf", "2026", uploader);
        ReflectionTestUtils.setField(uploader, "id", 1L);
        ReflectionTestUtils.setField(document, "id", 8L);
        when(documents.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(document));

        var result = controller.policies();

        assertEquals(1, result.size());
        assertEquals(8L, result.getFirst().id());
        assertEquals("leave-policy.pdf", result.getFirst().fileName());
        assertEquals("2026", result.getFirst().version());
        assertEquals(1L, result.getFirst().uploadedById());
        verify(documents).findAllByOrderByCreatedAtDesc();
    }
}
