package com.mahesh.empleave.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.holiday.HolidayRepository;
import com.mahesh.empleave.holiday.Holiday;
import com.mahesh.empleave.leave.LeaveService;
import com.mahesh.empleave.leave.LeaveApplication;
import com.mahesh.empleave.leavebalance.LeaveBalance;
import com.mahesh.empleave.leavebalance.LeaveBalanceRepository;
import com.mahesh.empleave.leavetype.LeaveType;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

class AiChatServiceTest {
    @Test
    void chromaFailureIsNotReportedAsInsufficientEvidence() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class); var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var model = mock(EmbeddingModel.class); var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        when(conversations.save(any())).thenReturn(new ChatConversation(user, "policy"));
        when(embeddings.getIfAvailable()).thenReturn(model); when(model.embed(anyString())).thenReturn(new float[] {1, 2, 3});
        when(chroma.query(any(), eq(5))).thenThrow(new ChromaUnavailableException("down"));
        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "How many annual leave days are provided?");
        assertTrue(reply.answer().startsWith("Policy search is temporarily unavailable"));
        assertFalse(reply.answer().startsWith("Insufficient evidence"));
        verify(chatClient, never()).getObject();
    }

    @Test
    void productionConstructorIsExplicitlyMarkedForSpringInjection() {
        var injectionConstructors = java.util.Arrays.stream(AiChatService.class.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .toList();

        assertEquals(1, injectionConstructors.size());
        assertEquals(11, injectionConstructors.getFirst().getParameterCount());
    }

    @Test
    void unsupportedQuestionReturnsInsufficientEvidenceAndDoesNotCallModel() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "What is the moon phase?");
        when(conversations.save(any())).thenReturn(conversation);
        when(chunks.findAll()).thenReturn(List.of());

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "What is the moon phase?");

        assertTrue(reply.answer().startsWith("Policy search embeddings are unavailable"));
        verify(chatClient, never()).getObject();
        verifyNoInteractions(chroma);
        verify(messages, times(2)).save(any());
    }

    @Test
    void genericPolicyWordsDoNotCreateEvidenceForUnsupportedLeaveType() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "parental leave");
        var document = new PolicyDocument("demo.pdf", null, user);
        when(conversations.save(any())).thenReturn(conversation);
        when(chunks.findAll()).thenReturn(List.of(new PolicyChunk(document, 0, 1,
                "This policy describes Annual Leave eligibility and the general leave application process.", "v1")));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "What is the parental leave policy? Explain the eligibility, duration, and benefits.");

        assertTrue(reply.answer().startsWith("Policy search embeddings are unavailable"));
        assertTrue(reply.citations().isEmpty());
        verify(chatClient, never()).getObject();
        verifyNoInteractions(chroma);
    }

    @Test
    void rawToolCallDetectionTreatsInternalJsonAsNonEmployeeFacing() {
        assertTrue(AiChatService.containsRawToolCall("{\"name\":\"leaveTypes\",\"parameters\":{}}"));
        assertFalse(AiChatService.containsRawToolCall("Annual Leave entitlement is 20 days."));
    }

    @Test
    void chromaVectorIdsSelectOnlyMatchingMysqlChunksAndProducePageCitation() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var model = mock(EmbeddingModel.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "annual entitlement");
        var document = new PolicyDocument("annual-policy.pdf", null, user);
        var selected = new PolicyChunk(document, 0, 2, "Annual Leave entitlement is 20 days.", "vector-annual");
        when(conversations.save(any())).thenReturn(conversation);
        when(embeddings.getIfAvailable()).thenReturn(model);
        when(model.embed(anyString())).thenReturn(new float[] { 1, 2, 3 });
        when(chroma.query(any(), eq(5))).thenReturn(List.of(
                new ChromaClient.QueryResult("vector-annual", 0.12),
                new ChromaClient.QueryResult("vector-annual", 0.15)));
        when(chunks.findByVectorIdIn(List.of("vector-annual"))).thenReturn(List.of(selected));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var context = service.retrieve("How many Annual Leave days are available?");

        assertEquals(1, context.size());
        assertTrue(context.get(0).content().contains("20 days"));
        verify(chunks).findByVectorIdIn(List.of("vector-annual"));
        verify(chunks, never()).findAll();
    }

    @Test
    void missingMysqlChunkForChromaIdReturnsNoEvidenceWithoutFullTableFallback() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var model = mock(EmbeddingModel.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "annual entitlement");
        when(conversations.save(any())).thenReturn(conversation);
        when(embeddings.getIfAvailable()).thenReturn(model);
        when(model.embed(anyString())).thenReturn(new float[] { 1, 2, 3 });
        when(chroma.query(any(), eq(5))).thenReturn(List.of(new ChromaClient.QueryResult("deleted-vector", 0.2)));
        when(chunks.findByVectorIdIn(List.of("deleted-vector"))).thenReturn(List.of());

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "How many Annual Leave days are available?");

        assertTrue(reply.answer().startsWith("Policy search indexing is being repaired"));
        assertTrue(reply.citations().isEmpty());
        verify(chunks).findByVectorIdIn(List.of("deleted-vector"));
        verify(chunks, never()).findAll();
        verify(chatClient, never()).getObject();
    }

    @Test
    void leaveHistoryToolReturnsNoApplicationsWithoutCallingRag() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "history");
        when(conversations.save(any())).thenReturn(conversation);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(leaves.historyFor(user)).thenReturn(List.of());

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me my leave application history.");

        assertTrue(reply.answer().contains("no leave applications"));
        assertTrue(reply.citations().isEmpty());
        verify(leaves).historyFor(user);
        verify(chatClient, never()).getObject();
    }

    @Test
    void leaveHistoryToolReturnsOnlyAuthenticatedEmployeesApplications() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var other = new AppUser("other@example.com", "hash", Role.EMPLOYEE, "Other");
        var conversation = new ChatConversation(user, "history");
        var application = new LeaveApplication(user, "Annual Leave", java.time.LocalDate.of(2099, 1, 5), java.time.LocalDate.of(2099, 1, 6), 2, "Demo");
        when(conversations.save(any())).thenReturn(conversation);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(leaves.historyFor(user)).thenReturn(List.of(application));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me my leave application history.");

        assertTrue(reply.answer().contains("Annual Leave"));
        assertTrue(reply.answer().contains("2099-01-05"));
        verify(leaves).historyFor(user);
        verify(leaves, never()).historyFor(other);
    }

    @Test
    void leaveHistoryFailureReturnsSafeMessageWithoutFabricatingRecords() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "history");
        when(conversations.save(any())).thenReturn(conversation);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(leaves.historyFor(user)).thenThrow(new IllegalStateException("database unavailable"));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me my leave application history.");

        assertTrue(reply.answer().contains("couldn't retrieve"));
        assertFalse(reply.answer().contains("Annual Leave"));
        assertTrue(reply.citations().isEmpty());
        verify(chatClient, never()).getObject();
    }

    @Test
    void upcomingHolidaysReturnOnlyConfiguredFutureRecordsWithoutCallingRag() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "holidays");
        var upcoming = new Holiday(LocalDate.now().plusDays(3), "Configured Festival");
        when(conversations.save(any())).thenReturn(conversation);
        when(holidays.findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31))).thenReturn(List.of(upcoming));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me the upcoming company holidays.");

        assertTrue(reply.answer().contains("Configured Festival"));
        assertFalse(reply.answer().contains("unconfigured"));
        assertTrue(reply.citations().isEmpty());
        verify(holidays).findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31));
        verify(chatClient, never()).getObject();
        verifyNoInteractions(embeddings, chroma);
    }

    @Test
    void upcomingHolidaysExcludePastRecordsBecauseRepositoryRangeStartsToday() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "holidays");
        when(conversations.save(any())).thenReturn(conversation);
        when(holidays.findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31)))
                .thenReturn(List.of(new Holiday(LocalDate.now().plusDays(1), "Future Holiday")));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me the upcoming company holidays.");

        assertTrue(reply.answer().contains("Future Holiday"));
        assertFalse(reply.answer().contains("Past Holiday"));
        verify(holidays).findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31));
    }

    @Test
    void noUpcomingHolidaysReturnsConfiguredEmptyStateWithoutCallingRag() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "holidays");
        when(conversations.save(any())).thenReturn(conversation);
        when(holidays.findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31))).thenReturn(List.of());

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me the upcoming company holidays.");

        assertEquals("There are no upcoming company holidays configured.", reply.answer());
        assertTrue(reply.citations().isEmpty());
        verify(chatClient, never()).getObject();
        verifyNoInteractions(embeddings, chroma);
    }

    @Test
    void upcomingHolidayRepositoryFailureReturnsSafeResponseWithoutFabricatingRecords() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "holidays");
        when(conversations.save(any())).thenReturn(conversation);
        when(holidays.findByHolidayDateBetween(LocalDate.now(), LocalDate.of(9999, 12, 31))).thenThrow(new IllegalStateException("database unavailable"));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "Show me the upcoming company holidays.");

        assertEquals("I couldn't retrieve the upcoming company holidays right now.", reply.answer());
        assertFalse(reply.answer().contains("Holiday"));
        assertTrue(reply.citations().isEmpty());
        verify(chatClient, never()).getObject();
        verifyNoInteractions(embeddings, chroma);
    }

    @Test
    void conversationMustBelongToAuthenticatedUser() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        when(conversations.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> service.ask(user, 99L, "What is the leave policy?"));
        verify(messages, never()).save(any());
    }

    @Test
    void leaveIsNotSubmittedWithoutConversationBoundConfirmation() {
        var conversations = mock(ChatConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class);
        var chroma = mock(ChromaClient.class);
        var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class);
        var balances = mock(LeaveBalanceRepository.class);
        var holidays = mock(HolidayRepository.class);
        var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "confirm leave");
        var application = new LeaveApplication(user, "Annual Leave", java.time.LocalDate.of(2099, 1, 5), java.time.LocalDate.of(2099, 1, 5), 1, "Demo");
        when(conversations.save(any())).thenReturn(conversation);
        when(chunks.findAll()).thenReturn(List.of());
        when(leaves.apply(anyString(), anyString(), any(), any(), anyString())).thenReturn(application);

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "I explicitly confirm: submit Annual Leave from 2099-01-05 through 2099-01-05, reason Demo.");

        assertTrue(reply.answer().startsWith("Policy search embeddings are unavailable"));
        verify(leaves, never()).apply(anyString(), anyString(), any(), any(), anyString());
        verify(chatClient, never()).getObject();
    }

    @Test
    void exactLeaveRequestIsPreparedBeforeRagWithoutCreatingApplication() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var confirmations = mock(PendingLeaveConfirmationRepository.class); var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "apply"); when(conversations.save(any())).thenReturn(conversation);
        when(leaves.validateApplication(user.getEmail(), "Annual Leave", LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10))).thenReturn(2);
        when(confirmations.findByConversationAndUser(conversation, user)).thenReturn(Optional.empty());
        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves, confirmations);

        var reply = service.ask(user, null, "Apply for 2 days of Annual Leave from 2026-11-09 to 2026-11-10 for personal work.");

        assertTrue(reply.answer().contains("ready for confirmation")); assertTrue(reply.answer().contains("Annual Leave")); assertTrue(reply.answer().contains("Working Days: 2"));
        verify(leaves, never()).apply(anyString(), anyString(), any(), any(), anyString()); verifyNoInteractions(embeddings, chroma); assertTrue(reply.citations().isEmpty());
    }

    @Test
    void yesSubmitsOnceAndRepeatedYesReturnsSameApplication() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var confirmations = mock(PendingLeaveConfirmationRepository.class); var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var conversation = new ChatConversation(user, "apply"); var pending = new PendingLeaveConfirmation(conversation, user, "Annual Leave", LocalDate.of(2099, 1, 5), LocalDate.of(2099, 1, 6), "Personal work");
        var application = mock(LeaveApplication.class); when(application.getId()).thenReturn(42L); when(application.getLeaveType()).thenReturn("Annual Leave"); when(application.getStartDate()).thenReturn(LocalDate.of(2099, 1, 5)); when(application.getEndDate()).thenReturn(LocalDate.of(2099, 1, 6)); when(application.getStatus()).thenReturn(com.mahesh.empleave.leave.LeaveStatus.PENDING);
        when(conversations.save(any())).thenReturn(conversation); when(confirmations.findForUpdate(conversation, user)).thenReturn(Optional.of(pending)); when(leaves.apply(user.getEmail(), "Annual Leave", LocalDate.of(2099, 1, 5), LocalDate.of(2099, 1, 6), "Personal work")).thenReturn(application);
        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves, confirmations);

        var first = service.ask(user, null, "YES"); var second = service.ask(user, first.conversationId(), "YES");

        assertTrue(first.answer().contains("#42")); assertTrue(second.answer().contains("#42")); verify(leaves, times(1)).apply(user.getEmail(), "Annual Leave", LocalDate.of(2099, 1, 5), LocalDate.of(2099, 1, 6), "Personal work");
    }
    @Test
    void personalBalanceQuestionUsesAuthenticatedUsersDatabaseBalanceWithoutPolicyCitation() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var mahesh = new AppUser("mahesh@example.com", "hash", Role.EMPLOYEE, "Mahesh Savkar");
        var conversation = new ChatConversation(mahesh, "balance");
        var annual = new LeaveType("Annual Leave");
        when(conversations.save(any())).thenReturn(conversation);
        when(users.findByEmail(mahesh.getEmail())).thenReturn(Optional.of(mahesh));
        when(balances.findByUser(mahesh)).thenReturn(List.of(new LeaveBalance(mahesh, annual, new BigDecimal("3.00"))));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(mahesh, null, "How many leave days do I have remaining?");

        assertTrue(reply.answer().contains("Annual Leave: 3.00"));
        assertTrue(reply.citations().isEmpty());
        verify(balances).findByUser(mahesh);
        verifyNoInteractions(embeddings, chroma, chatClient);
    }

    @Test
    void policyRuleQuestionsReachRagInsteadOfPersonalBalanceShortcut() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        when(conversations.save(any())).thenAnswer(invocation -> new ChatConversation(user, "policy"));
        when(embeddings.getIfAvailable()).thenReturn(null);
        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);

        for (var question : List.of(
                "Will pending leave requests be deducted from my leave balance?",
                "If my manager has not approved my leave, will my balance be deducted?",
                "How many sick leave days does the policy provide?")) {
            var reply = service.ask(user, null, question);
            assertTrue(reply.answer().startsWith("Policy search embeddings are unavailable"), question);
        }
        verifyNoInteractions(balances, chroma, chatClient);
    }

    @Test
    void personalBalanceQuestionCannotReadAnotherEmployeesBalance() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var mahesh = new AppUser("mahesh@example.com", "hash", Role.EMPLOYEE, "Mahesh Savkar");
        var other = new AppUser("other@example.com", "hash", Role.EMPLOYEE, "Other Employee");
        var annual = new LeaveType("Annual Leave");
        when(conversations.save(any())).thenReturn(new ChatConversation(other, "balance"));
        when(users.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(balances.findByUser(other)).thenReturn(List.of(new LeaveBalance(other, annual, new BigDecimal("8.00"))));
        when(balances.findByUser(mahesh)).thenReturn(List.of(new LeaveBalance(mahesh, annual, new BigDecimal("3.00"))));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(other, null, "How many leave days do I have remaining?");

        assertTrue(reply.answer().contains("8.00"));
        assertFalse(reply.answer().contains("3.00"));
        verify(balances).findByUser(other);
        verify(balances, never()).findByUser(mahesh);
    }

    @Test
    void personalBalanceFailureDoesNotInventABalance() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        when(conversations.save(any())).thenReturn(new ChatConversation(user, "balance"));
        when(users.findByEmail(user.getEmail())).thenThrow(new IllegalStateException("database unavailable"));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var reply = service.ask(user, null, "How many leave days do I have remaining?");

        assertEquals("I couldn't retrieve your current leave balance right now.", reply.answer());
        assertTrue(reply.citations().isEmpty());
        verifyNoInteractions(embeddings, chroma, chatClient);
    }

    @Test
    void annualEntitlementQuestionStillUsesPolicyRag() {
        var conversations = mock(ChatConversationRepository.class); var messages = mock(ChatMessageRepository.class); var chunks = mock(PolicyChunkRepository.class);
        var embeddings = mock(ObjectProvider.class); var chroma = mock(ChromaClient.class); var chatClient = mock(ObjectProvider.class);
        var users = mock(UserRepository.class); var balances = mock(LeaveBalanceRepository.class); var holidays = mock(HolidayRepository.class); var leaves = mock(LeaveService.class);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var document = new PolicyDocument("annual-policy.pdf", null, user);
        var model = mock(EmbeddingModel.class);
        when(embeddings.getIfAvailable()).thenReturn(model);
        when(model.embed(anyString())).thenReturn(new float[] {1, 2, 3});
        when(chroma.query(any(), eq(5))).thenReturn(List.of(new ChromaClient.QueryResult("annual-vector", 0.1)));
        when(chunks.findByVectorIdIn(List.of("annual-vector"))).thenReturn(List.of(new PolicyChunk(document, 0, 2, "Annual Leave entitlement is 20 days each year.", "annual-vector")));

        var service = new AiChatService(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves);
        var context = service.retrieve("How many annual leave days am I entitled to each year?");

        assertEquals(1, context.size());
        verify(chroma).query(any(), eq(5));
        verify(balances, never()).findByUser(any());
    }
}
