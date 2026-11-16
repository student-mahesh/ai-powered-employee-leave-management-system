package com.mahesh.empleave.ai;

import com.mahesh.empleave.holiday.HolidayRepository;
import com.mahesh.empleave.leave.LeaveService;
import com.mahesh.empleave.leavebalance.LeaveBalanceRepository;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AiChatService {
    private static final String INSUFFICIENT_EVIDENCE = "Insufficient evidence in the uploaded HR policies to answer that question.";
    private static final long MIN_EVIDENCE_SCORE = 1L;
    private static final Pattern RAW_TOOL_CALL = Pattern.compile("(?is)(?:\\{\\s*)?['\"]?(?:name|tool|function)['\"]?\\s*[:=]\\s*['\"]?(?:leaveTypes|leaveBalance|leaveHistory|companyHolidays|applyLeave)['\"]?.*(?:parameters|arguments|tool_call)");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern LEAVE_TYPE_PATTERN = Pattern.compile("(?i)\\b([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s+Leave\\b");
    private static final Pattern REASON_PATTERN = Pattern.compile("(?i)\\breason\\s*(?:is|:|-)?\\s*(.+?)(?:\\.|$)");
    private static final Pattern LEAVE_REQUEST_PATTERN = Pattern.compile("(?i)\\b(?:apply|submit|request)\\s+(?:for\\s+\\d+\\s+days?\\s+of\\s+)?([A-Za-z]+(?:\\s+[A-Za-z]+)?\\s+Leave)\\s+(?:from|between)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(?:to|through|and)\\s+(\\d{4}-\\d{2}-\\d{2})(?:\\s+(?:for|reason(?:\\s+is)?[:\\s-]*)\\s+(.+?))?\\.?$");
    private static final Pattern CONFIRM_YES = Pattern.compile("(?i)^\\s*(?:yes|y|confirm)\\s*[!.]?\\s*$");
    private static final Pattern CONFIRM_NO = Pattern.compile("(?i)^\\s*(?:no|n|cancel)\\s*[!.]?\\s*$");
    private static final Pattern LEAVE_HISTORY_REQUEST = Pattern.compile("(?i)\\b(?:leave application history|leave history|past leave applications|my leave requests)\\b");
    private static final Pattern LEAVE_BALANCE_REQUEST = Pattern.compile("(?i)(?:\\bmy\\b.*\\b(?:leave\\s+)?balances?\\b|\\b(?:how many|what)\\b.*\\bleave\\s+days?\\b.*\\b(?:do i have|remaining|left)\\b|\\b(?:leave\\s+)?balance\\b.*\\b(?:remaining|left)\\b)");
    private static final Pattern POLICY_RULE_REQUEST = Pattern.compile("(?i)\\b(?:pending|approv(?:e|ed|al)|deduct(?:ed|ion)?|cancel(?:led|lation)?|eligib(?:le|ility)|entitlement|carry[- ]?forward|leave rules?)\\b");
    private static final Pattern UPCOMING_HOLIDAYS_REQUEST = Pattern.compile("(?i)(?:\\b(?:upcoming|next|future|company|office|public|list|show|what|which|are\\s+there)\\b.*\\bholidays?\\b|\\bholidays?\\b.*\\b(?:upcoming|next|future|company|office|public|list|show)\\b)");
    private static final LocalDate LAST_SUPPORTED_HOLIDAY_DATE = LocalDate.of(9999, 12, 31);
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final PolicyChunkRepository chunks;
    private final ObjectProvider<EmbeddingModel> embeddings;
    private final ChromaClient chroma;
    private final ObjectProvider<ChatClient.Builder> chatClient;
    private final UserRepository users;
    private final LeaveBalanceRepository balances;
    private final HolidayRepository holidays;
    private final LeaveService leaves;
    private final PendingLeaveConfirmationRepository confirmations;

    public AiChatService(ChatConversationRepository conversations, ChatMessageRepository messages, PolicyChunkRepository chunks,
                         ObjectProvider<EmbeddingModel> embeddings, ChromaClient chroma, ObjectProvider<ChatClient.Builder> chatClient,
                         UserRepository users, LeaveBalanceRepository balances, HolidayRepository holidays, LeaveService leaves) {
        this(conversations, messages, chunks, embeddings, chroma, chatClient, users, balances, holidays, leaves, null);
    }

    /**
     * Production injection constructor.  The shorter overload above remains for
     * existing unit tests that exercise non-confirmation chat paths; it must not
     * be selected by Spring because confirmation state is required in production.
     */
    @Autowired
    public AiChatService(ChatConversationRepository conversations, ChatMessageRepository messages, PolicyChunkRepository chunks,
                         ObjectProvider<EmbeddingModel> embeddings, ChromaClient chroma, ObjectProvider<ChatClient.Builder> chatClient,
                         UserRepository users, LeaveBalanceRepository balances, HolidayRepository holidays, LeaveService leaves,
                         PendingLeaveConfirmationRepository confirmations) {
        this.conversations = conversations;
        this.messages = messages;
        this.chunks = chunks;
        this.embeddings = embeddings;
        this.chroma = chroma;
        this.chatClient = chatClient;
        this.users = users;
        this.balances = balances;
        this.holidays = holidays;
        this.leaves = leaves;
        this.confirmations = confirmations;
    }

    @Transactional
    public ChatReply ask(AppUser user, Long conversationId, String question) {
        if (question == null || question.isBlank() || question.length() > 4000) throw new IllegalArgumentException("Question must contain 1 to 4000 characters");
        var conversationStarted = System.nanoTime();
        var conversation = conversationId == null
                ? conversations.save(new ChatConversation(user, question.substring(0, Math.min(question.length(), 120))))
                : conversations.findByIdAndUser(conversationId, user).orElseThrow(() -> new SecurityException("Conversation does not belong to the authenticated user"));
        log.info("ai_chat_conversation durationMs={} created={}", elapsedMs(conversationStarted), conversationId == null);
        var userMessageStarted = System.nanoTime();
        messages.save(new ChatMessage(conversation, "user", question, null));
        log.info("ai_chat_user_message_persistence durationMs={}", elapsedMs(userMessageStarted));
        if (confirmations != null && CONFIRM_YES.matcher(question).matches()) return submitPendingConfirmation(user, conversation);
        if (confirmations != null && CONFIRM_NO.matcher(question).matches()) return cancelPendingConfirmation(user, conversation);
        var leaveRequest = parseLeaveRequest(question);
        if (leaveRequest.isPresent()) return prepareLeaveConfirmation(user, conversation, leaveRequest.get());
        if (!POLICY_RULE_REQUEST.matcher(question).find() && LEAVE_BALANCE_REQUEST.matcher(question).find()) {
            var tools = new AiTools(user.getEmail(), users, balances, holidays, leaves);
            String answer;
            try {
                var result = tools.leaveBalance();
                answer = result == null || result.isBlank()
                        ? "I couldn't retrieve a current leave balance for you."
                        : "Your current leave balances: " + result;
            } catch (RuntimeException ex) {
                log.warn("ai_chat_leave_balance_failed type={}", ex.getClass().getSimpleName());
                answer = "I couldn't retrieve your current leave balance right now.";
            }
            return saveAssistantReply(conversation, answer);
        }
        if (LEAVE_HISTORY_REQUEST.matcher(question).find()) {
            var tools = new AiTools(user.getEmail(), users, balances, holidays, leaves);
            String answer;
            try {
                var result = tools.leaveHistory();
                answer = result == null || result.isBlank()
                        ? "You have no leave applications in your history."
                        : "Your leave application history: " + result;
            } catch (RuntimeException ex) {
                log.warn("ai_chat_leave_history_failed type={}", ex.getClass().getSimpleName());
                answer = "I couldn't retrieve your leave application history right now.";
            }
            var assistantMessageStarted = System.nanoTime();
            messages.save(new ChatMessage(conversation, "assistant", answer, ""));
            log.info("ai_chat_assistant_message_persistence durationMs={}", elapsedMs(assistantMessageStarted));
            return new ChatReply(conversation.getId(), answer, List.of());
        }
        if (UPCOMING_HOLIDAYS_REQUEST.matcher(question).find()) {
            var tools = new AiTools(user.getEmail(), users, balances, holidays, leaves);
            String answer;
            try {
                var result = tools.companyHolidays(LocalDate.now().toString(), LAST_SUPPORTED_HOLIDAY_DATE.toString());
                answer = result == null || result.isBlank()
                        ? "There are no upcoming company holidays configured."
                        : "Upcoming company holidays: " + result;
            } catch (RuntimeException ex) {
                log.warn("ai_chat_company_holidays_failed type={}", ex.getClass().getSimpleName());
                answer = "I couldn't retrieve the upcoming company holidays right now.";
            }
            var assistantMessageStarted = System.nanoTime();
            messages.save(new ChatMessage(conversation, "assistant", answer, ""));
            log.info("ai_chat_assistant_message_persistence durationMs={}", elapsedMs(assistantMessageStarted));
            return new ChatReply(conversation.getId(), answer, List.of());
        }
        var retrieval = retrieveDetailed(question);
        var context = retrieval.context();
        var citations = context.stream().map(item -> item.document().getFileName() + "#page-" + item.page()).distinct().toList();
        if (context.isEmpty()) {
            var answer = retrieval.status().message;
            var assistantMessageStarted = System.nanoTime();
            messages.save(new ChatMessage(conversation, "assistant", answer, ""));
            log.info("ai_chat_assistant_message_persistence durationMs={}", elapsedMs(assistantMessageStarted));
            return new ChatReply(conversation.getId(), answer, citations);
        }
        var prompt = "Use only the policy context for policy claims. If it is insufficient, say so. Never approve or reject leave. " +
                "A leave application may be submitted only through the confirmation-gated tool.\n\nPOLICY CONTEXT:\n" +
                context.stream().map(Context::content).collect(Collectors.joining("\n---\n"));
        var tools = new AiTools(user.getEmail(), users, balances, holidays, leaves);
        var generationStarted = System.nanoTime();
        var answer = chatClient.getObject().build().prompt().system(prompt).user(question).tools(tools).call().content();
        log.info("ai_chat_ollama_generation durationMs={}", elapsedMs(generationStarted));
        if (answer == null || answer.isBlank()) throw new IllegalStateException("AI provider returned an empty answer");
        if (containsRawToolCall(answer)) {
            answer = INSUFFICIENT_EVIDENCE;
            citations = List.of();
        }
        var assistantMessageStarted = System.nanoTime();
        messages.save(new ChatMessage(conversation, "assistant", answer, String.join(", ", citations)));
        log.info("ai_chat_assistant_message_persistence durationMs={}", elapsedMs(assistantMessageStarted));
        return new ChatReply(conversation.getId(), answer, citations);
    }

    private Optional<LeaveCommand> parseLeaveRequest(String question) {
        var match = LEAVE_REQUEST_PATTERN.matcher(question.trim());
        if (!match.matches()) return Optional.empty();
        var reason = match.group(4);
        if (reason == null || reason.isBlank()) return Optional.empty();
        return Optional.of(new LeaveCommand(match.group(1).trim(), match.group(2), match.group(3), reason.trim()));
    }

    private ChatReply prepareLeaveConfirmation(AppUser user, ChatConversation conversation, LeaveCommand command) {
        String answer;
        try {
            int days = leaves.validateApplication(user.getEmail(), command.leaveType(), command.startDate(), command.endDate());
            var pending = confirmations.findByConversationAndUser(conversation, user)
                    .orElseGet(() -> new PendingLeaveConfirmation(conversation, user, command.leaveType(), command.startDate(), command.endDate(), command.reason()));
            if (pending.getId() != null) pending.replace(command.leaveType(), command.startDate(), command.endDate(), command.reason());
            confirmations.save(pending);
            answer = "Your leave application is ready for confirmation.\n\nLeave Type: " + command.leaveType() + "\nFrom: " + command.startDate() + "\nTo: " + command.endDate() + "\nWorking Days: " + days + "\nReason: " + command.reason() + "\n\nWould you like me to submit this leave application? Please reply YES to confirm or NO to cancel.";
        } catch (RuntimeException ex) { answer = "Leave application was not prepared: " + ex.getMessage(); }
        return saveAssistantReply(conversation, answer);
    }

    private ChatReply submitPendingConfirmation(AppUser user, ChatConversation conversation) {
        String answer;
        try {
            var pending = confirmations.findForUpdate(conversation, user).orElseThrow(() -> new IllegalStateException("There is no pending leave application to confirm."));
            if (pending.getStatus() == PendingLeaveConfirmation.Status.SUBMITTED) {
                var application = pending.getApplication();
                answer = submittedAnswer(application);
            } else if (pending.getStatus() != PendingLeaveConfirmation.Status.WAITING) {
                answer = "There is no pending leave application to confirm.";
            } else {
                var application = leaves.apply(user.getEmail(), pending.getLeaveType(), pending.getStartDate(), pending.getEndDate(), pending.getReason());
                pending.submitted(application);
                answer = submittedAnswer(application);
            }
        } catch (RuntimeException ex) { answer = "Leave application was not submitted: " + ex.getMessage(); }
        return saveAssistantReply(conversation, answer);
    }

    private ChatReply cancelPendingConfirmation(AppUser user, ChatConversation conversation) {
        String answer;
        try {
            var pending = confirmations.findForUpdate(conversation, user).orElseThrow(() -> new IllegalStateException("There is no pending leave application to cancel."));
            if (pending.getStatus() == PendingLeaveConfirmation.Status.WAITING) pending.cancel();
            answer = "The pending leave application has been cancelled. No leave request was created.";
        } catch (RuntimeException ex) { answer = "There is no pending leave application to cancel."; }
        return saveAssistantReply(conversation, answer);
    }

    private ChatReply saveAssistantReply(ChatConversation conversation, String answer) {
        messages.save(new ChatMessage(conversation, "assistant", answer, ""));
        return new ChatReply(conversation.getId(), answer, List.of());
    }

    private static String submittedAnswer(com.mahesh.empleave.leave.LeaveApplication application) {
        return "Leave application #" + application.getId() + " submitted: " + application.getLeaveType() + " from " + application.getStartDate() + " to " + application.getEndDate() + " (" + application.getStatus() + "). The AI cannot approve or reject it.";
    }

    List<Context> retrieve(String question) {
        return retrieveDetailed(question).context();
    }

    private RetrievalResult retrieveDetailed(String question) {
        var retrievalStarted = System.nanoTime();
        var embeddingStarted = System.nanoTime();
        var model = embeddings.getIfAvailable();
        if (model == null) {
            log.info("ai_chat_retrieval durationMs={} chromaReturned=0 mysqlMatched=0 evidenceAccepted=0 evidenceRejected=0", elapsedMs(retrievalStarted));
            return RetrievalResult.failure(RetrievalStatus.EMBEDDING_UNAVAILABLE);
        }
        float[] vector;
        try {
            vector = model.embed(question);
            log.info("ai_chat_embedding_generation durationMs={} dimensions={}", elapsedMs(embeddingStarted), vector.length);
        } catch (Exception ex) {
            log.info("ai_chat_retrieval durationMs={} chromaReturned=0 mysqlMatched=0 evidenceAccepted=0 evidenceRejected=0", elapsedMs(retrievalStarted));
            return RetrievalResult.failure(RetrievalStatus.EMBEDDING_FAILED);
        }
        var chromaStarted = System.nanoTime();
        List<ChromaClient.QueryResult> chromaResults;
        try {
            chromaResults = chroma.query(vector, 5);
        } catch (ChromaUnavailableException ex) {
            log.warn("ai_chat_chroma_unavailable");
            return RetrievalResult.failure(RetrievalStatus.CHROMA_UNAVAILABLE);
        }
        log.info("ai_chat_chroma_query durationMs={} returnedCount={}", elapsedMs(chromaStarted), chromaResults.size());
        var vectorIds = chromaResults.stream().map(ChromaClient.QueryResult::id).filter(Objects::nonNull).distinct().toList();
        if (vectorIds.isEmpty()) {
            log.info("ai_chat_retrieval durationMs={} chromaReturned={} mysqlMatched=0 evidenceAccepted=0 evidenceRejected=0", elapsedMs(retrievalStarted), chromaResults.size());
            return RetrievalResult.failure(RetrievalStatus.NO_EVIDENCE);
        }
        var chunkStarted = System.nanoTime();
        var matchedChunks = chunks.findByVectorIdIn(vectorIds);
        log.info("ai_chat_mysql_policy_chunk_retrieval durationMs={} count={}", elapsedMs(chunkStarted), matchedChunks.size());
        if (matchedChunks.isEmpty()) {
            log.warn("ai_chat_policy_index_inconsistent chromaIds={} mysqlMatched=0", vectorIds.size());
            return RetrievalResult.failure(RetrievalStatus.INDEX_INCONSISTENT);
        }
        var stopWords = Set.of("what", "does", "the", "say", "about", "how", "many", "is", "are", "a", "an", "and", "or",
                "company", "policy", "leave", "days", "test", "employee", "demo", "system", "according",
                "provide", "provided", "rule", "rules", "tell", "me", "can", "you", "eligibility", "duration",
                "benefit", "benefits", "available", "information", "contain", "contains", "explain", "question");
        var tokens = Arrays.stream(question.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> token.length() > 2 && !stopWords.contains(token)).collect(Collectors.toSet());
        var scoringStarted = System.nanoTime();
        var byVectorId = matchedChunks.stream().filter(chunk -> chunk.getVectorId() != null)
                .collect(Collectors.toMap(PolicyChunk::getVectorId, chunk -> chunk, (first, ignored) -> first));
        var retrievedChunks = vectorIds.stream().map(byVectorId::get).filter(Objects::nonNull).toList();
        var context = retrievedChunks.stream().map(chunk -> new Context(chunk.getDocument(), chunk.getSourcePage(), chunk.getContent(),
                        tokens.stream().filter(token -> chunk.getContent().toLowerCase(Locale.ROOT).contains(token)).count()))
                .filter(item -> item.score() >= MIN_EVIDENCE_SCORE)
                .limit(5).toList();
        var evidenceRejected = Math.max(0, chromaResults.size() - context.size());
        log.info("ai_chat_lexical_scoring durationMs={} evidenceAccepted={} evidenceRejected={}", elapsedMs(scoringStarted), context.size(), evidenceRejected);
        log.info("ai_chat_retrieval durationMs={} chromaReturned={} mysqlMatched={} evidenceAccepted={} evidenceRejected={}", elapsedMs(retrievalStarted), chromaResults.size(), matchedChunks.size(), context.size(), evidenceRejected);
        return context.isEmpty() ? RetrievalResult.failure(RetrievalStatus.NO_EVIDENCE)
                : new RetrievalResult(context, RetrievalStatus.OK);
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    static boolean containsRawToolCall(String answer) {
        return answer != null && RAW_TOOL_CALL.matcher(answer).find();
    }

    public record ChatReply(Long conversationId, String answer, List<String> citations) { }
    private record LeaveCommand(String leaveType, String startDateText, String endDateText, String reason) {
        LocalDate startDate() { return LocalDate.parse(startDateText); }
        LocalDate endDate() { return LocalDate.parse(endDateText); }
    }
    record Context(PolicyDocument document, Integer page, String content, long score) { }

    private record RetrievalResult(List<Context> context, RetrievalStatus status) {
        static RetrievalResult failure(RetrievalStatus status) { return new RetrievalResult(List.of(), status); }
    }

    private enum RetrievalStatus {
        OK(""),
        NO_EVIDENCE(INSUFFICIENT_EVIDENCE),
        CHROMA_UNAVAILABLE("Policy search is temporarily unavailable. Please try again later."),
        EMBEDDING_UNAVAILABLE("Policy search embeddings are unavailable. Please try again later."),
        EMBEDDING_FAILED("Policy search embeddings could not be generated. Please try again later."),
        INDEX_INCONSISTENT("Policy search indexing is being repaired. Please try again later.");
        private final String message;
        RetrievalStatus(String message) { this.message = message; }
    }
}
