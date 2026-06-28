package com.Service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

@Service
public class TextsplitterService {

    private final ChatClient chatClient;

    public TextsplitterService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // ─────────────────────────────────────────────
    // Split document into chunks of 1500 chars
    // with 100-char overlap between chunks
    // ─────────────────────────────────────────────
    public List<TextSegment> splitData(Document document) {

        var splitter = DocumentSplitters.recursive(1500, 100);

        return splitter.split(document);
    }

    // ─────────────────────────────────────────────
    // Send a single chunk to AI for code review
    // ─────────────────────────────────────────────
    public String aiReview(String code) {

        String systemPrompt = """
                You are a Principal Software Architect,
                Senior Java Developer,
                Spring Boot Expert,
                Security Auditor,
                and Performance Engineer.

                Your job is to perform professional repository code reviews.

                Rules:

                - Be extremely technical and specific.
                - Detect architectural issues.
                - Detect security vulnerabilities.
                - Detect performance bottlenecks.
                - Detect code smells and maintainability issues.
                - Detect Spring Boot best practice violations.
                - Suggest exact fixes.
                - Never give generic advice.
                - If code quality is good, explain why.
                - Provide actionable recommendations.
                - Focus only on issues actually present in the code.
                - Do not invent problems.
                - Return clean readable text.
                """;

        String userPrompt = """
                Analyze the following Java/Spring Boot source code.

                Create a professional code review report.

                ==================================================
                PROJECT SUMMARY
                ==================================================

                Explain what this code appears to do.

                ==================================================
                ARCHITECTURE REVIEW
                ==================================================

                Review:

                - Layer separation
                - Dependency Injection
                - Service design
                - Controller design
                - Scalability
                - Maintainability

                ==================================================
                CODE QUALITY ISSUES
                ==================================================

                For each issue provide:

                Severity:
                Problem:
                Why:
                Fix:

                ==================================================
                SECURITY ISSUES
                ==================================================

                Check for:

                - SQL Injection
                - XSS
                - CSRF
                - Hardcoded secrets
                - Missing validation
                - Authentication issues
                - Authorization issues
                - Sensitive data exposure

                For each issue provide:

                Severity:
                Problem:
                Why:
                Fix:

                ==================================================
                PERFORMANCE ISSUES
                ==================================================

                Check for:

                - Inefficient loops
                - Memory waste
                - Repeated API calls
                - Large object creation
                - Blocking operations
                - Database inefficiencies

                For each issue provide:

                Severity:
                Problem:
                Why:
                Fix:

                ==================================================
                SPRING BOOT BEST PRACTICES
                ==================================================

                Review:

                - Controller design
                - Service design
                - Repository usage
                - Exception handling
                - Logging
                - Configuration management

                ==================================================
                BUG RISKS
                ==================================================

                Find possible:

                - NullPointerException
                - Resource leaks
                - Concurrency issues
                - Runtime exceptions

                For each issue provide:

                Severity:
                Problem:
                Why:
                Fix:

                ==================================================
                REFACTORING SUGGESTIONS
                ==================================================

                List practical improvements.

                ==================================================
                SCORECARD
                ==================================================

                Architecture: X/10
                Code Quality: X/10
                Security: X/10
                Performance: X/10
                Maintainability: X/10

                ==================================================
                FINAL VERDICT
                ==================================================

                State whether the code is:

                PRODUCTION READY

                or

                NEEDS IMPROVEMENT

                and explain why.

                ==================================================
                SOURCE CODE
                ==================================================

                %s
                """.formatted(code);

        try {

            String response = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return response != null
                    ? response
                    : "AI returned an empty review.";

        } catch (Exception e) {

            e.printStackTrace();

            return """
                    AI Review Failed

                    Reason:
                    %s
                    """.formatted(e.getMessage());
        }
    }
    
    // <<..............final response ............>>>
    public String finalRepositoryReview(String chunkReviews) {

        String systemPrompt = """
                You are a Principal Software Architect.

                Your task is to analyze multiple chunk-level code reviews
                from the same repository and generate a single final review.

                Focus on:
                - Architecture
                - Security
                - Performance
                - Maintainability
                - Production readiness

                Provide one consolidated report.
                """;

        String userPrompt = """
                The following reviews were generated from different parts
                of the same repository.

                Create ONE final repository review.

                Include:

                ==================================================
                PROJECT SUMMARY
                ==================================================

                ==================================================
                ARCHITECTURE ANALYSIS
                ==================================================

                ==================================================
                SECURITY ANALYSIS
                ==================================================

                ==================================================
                PERFORMANCE ANALYSIS
                ==================================================

                ==================================================
                TOP ISSUES
                ==================================================

                ==================================================
                REFACTORING ROADMAP
                ==================================================

                ==================================================
                SCORECARD
                ==================================================

                Architecture: X/10
                Security: X/10
                Performance: X/10
                Maintainability: X/10

                ==================================================
                FINAL VERDICT
                ==================================================

                Chunk Reviews:

                %s
                """.formatted(chunkReviews);

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
    
    
}