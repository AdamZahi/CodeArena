package com.codearena.module7_coaching.service;

import com.codearena.module7_coaching.dto.AiRequest;
import com.codearena.module7_coaching.dto.AiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${huggingface.api-key:}")
    private String huggingFaceApiKey;

    // HF Router — OpenAI-compatible chat completions endpoint (free tier)
    private static final String HF_CHAT_URL = "https://router.huggingface.co/v1/chat/completions";
    // Confirmed available model on HF Inference Providers (from /v1/models)
    private static final String HF_MODEL = "Qwen/Qwen3-8B";

    public AiResponse processRequest(AiRequest request) {
        try {
            String prompt = buildPrompt(request);
            String aiContent = callHuggingFace(prompt);

            return AiResponse.builder()
                    .success(true)
                    .content(aiContent)
                    .mode(request.getMode())
                    .build();
        } catch (Exception e) {
            System.err.println("AI processing error: " + e.getMessage());
            e.printStackTrace();
            return AiResponse.builder()
                    .success(false)
                    .error("❌ ERREUR API : Impossible de générer la réponse.\n\nDétail : " + e.getMessage())
                    .mode(request.getMode())
                    .build();
        }
    }

    private String buildPrompt(AiRequest request) {
        String mode = request.getMode() != null ? request.getMode() : "CHAT";

        switch (mode) {
            case "SESSION_PLAN":
                return buildSessionPlanPrompt(request);
            case "QUIZ_GENERATE":
                return buildQuizPrompt(request);
            case "CODE_EXPLAIN":
                return buildCodeExplainPrompt(request);
            case "CODE_REVIEW":
                return buildCodeReviewPrompt(request);
            case "PRACTICE_EXERCISE":
                return buildPracticeExercisePrompt(request);
            case "DEBUG_HELP":
                return buildDebugHelpPrompt(request);
            case "CHAT":
            default:
                return buildChatPrompt(request);
        }
    }

    private String buildSessionPlanPrompt(AiRequest request) {
        return String.format("""
            You are an expert coding coach and curriculum designer for the "Code Arena" platform.
            Generate a detailed, structured coaching session plan in Markdown format.

            **Session Parameters:**
            - Topic: %s
            - Programming Language: %s
            - Level: %s
            - Duration: %d minutes

            **Please include the following sections:**

            ## 🎯 Session Title
            A catchy, descriptive title for the session.

            ## 📋 Learning Objectives
            3-5 clear, measurable learning objectives.

            ## 📅 Session Agenda
            A detailed minute-by-minute agenda with time allocations.

            ## 💻 Code Exercises
            2-3 hands-on coding exercises with:
            - Exercise description
            - Starter code
            - Expected solution
            - Key learning points

            ## 💡 Discussion Points
            Questions to engage learners and check understanding.

            ## 📝 Homework / Follow-up
            2-3 assignments for learners to practice after the session.

            ## 🏆 Success Criteria
            How to measure if the session was successful.

            Make the content practical, engaging, and appropriate for the specified level.
            Use code blocks with proper syntax highlighting for the specified language.
            """,
                request.getTopic() != null ? request.getTopic() : "General Programming",
                request.getLanguage() != null ? request.getLanguage() : "JAVA",
                request.getLevel() != null ? request.getLevel() : "INTERMEDIAIRE",
                request.getDurationMinutes() != null ? request.getDurationMinutes() : 60
        );
    }

    private String buildQuizPrompt(AiRequest request) {
        int count = request.getQuestionCount() != null ? request.getQuestionCount() : 5;
        return String.format("""
            You are an expert programming instructor for the "Code Arena" platform.
            Generate %d quiz questions in a strict JSON format.

            **Quiz Parameters:**
            - Topic: %s
            - Programming Language: %s
            - Difficulty: %s

            **Return ONLY valid strict JSON** in this exact format (do not wrap in markdown quotes, no extra text, just raw JSON array/object starting with {):
            {
              "quizTitle": "Quiz title here",
              "quizDescription": "Brief description",
              "questions": [
                {
                  "content": "The question text",
                  "type": "MCQ",
                  "options": "Option A|||Option B|||Option C|||Option D",
                  "correctAnswer": "The correct option text exactly as written",
                  "explanation": "Why this is correct",
                  "codeSnippet": "optional code snippet or null",
                  "points": 10
                }
              ]
            }

            Mix question types: MCQ (multiple choice), CODE_ANALYSIS (analyzing code output), CODE_COMPLETION (fill the blank).
            For CODE_ANALYSIS and CODE_COMPLETION types, always include a codeSnippet.
            Make questions progressively harder.
            Ensure all code is syntactically correct for the specified language.
            """,
                count,
                request.getTopic() != null ? request.getTopic() : "General Programming",
                request.getLanguage() != null ? request.getLanguage() : "JAVA",
                request.getLevel() != null ? request.getLevel() : "MEDIUM"
        );
    }

    private String buildChatPrompt(AiRequest request) {
        String context = request.getContext() != null ? request.getContext() : "";
        return String.format("""
            You are "ARIA" (Artificial Reasoning & Instructional AI), a specialized AI assistant 
            for coding coaches on the "Code Arena" platform. You help coaches become better educators.

            Your expertise includes:
            - Teaching methodologies for programming
            - Curriculum design and session planning
            - Student engagement strategies
            - Code review best practices
            - Explaining complex concepts simply
            - Debugging and problem-solving pedagogy

            Respond in a helpful, professional, and encouraging tone.
            Use markdown formatting for clarity.
            Include code examples when relevant.

            %s

            Coach's question: %s
            """,
                context.isEmpty() ? "" : "Previous context: " + context,
                request.getMessage() != null ? request.getMessage() : "Hello, how can you help me?"
        );
    }

    // ═══════ PARTICIPANT AI CODE MENTOR PROMPTS ═══════

    private String buildCodeExplainPrompt(AiRequest request) {
        String depth = request.getLevel() != null ? request.getLevel() : "intermediate";
        return String.format("""
            You are a patient, expert programming tutor on the "Code Arena" platform.
            A student has submitted code and wants a clear, step-by-step explanation.

            **Code to explain:**
            ```%s
            %s
            ```

            **Explanation depth:** %s

            **Provide your explanation in Markdown format with:**

            ## 🔍 Code Overview
            A brief summary of what this code does and its purpose.

            ## 📝 Line-by-Line Breakdown
            Go through each significant line/block and explain:
            - What it does
            - Why it's written that way
            - Any important concepts it demonstrates

            ## 🧠 Key Concepts
            List the programming concepts used (e.g., loops, recursion, OOP, etc.) with brief explanations.

            ## 💡 How It All Fits Together
            Explain the overall flow and logic of the code.

            ## ⚡ Quick Tips
            2-3 tips for the student related to this code.

            Adjust your language complexity based on the depth level:
            - beginner: Explain everything, assume no prior knowledge
            - intermediate: Focus on logic and patterns, skip basic syntax
            - advanced: Focus on performance, edge cases, and advanced patterns

            Use clear formatting, code annotations, and analogies where helpful.
            """,
                request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java",
                request.getMessage() != null ? request.getMessage() : "// No code provided",
                depth
        );
    }

    private String buildCodeReviewPrompt(AiRequest request) {
        String focus = request.getContext() != null ? request.getContext() : "Full review";
        return String.format("""
            You are a senior software engineer performing a professional code review on the "Code Arena" platform.
            Review the following code and provide constructive, actionable feedback.

            **Code to review:**
            ```%s
            %s
            ```

            **Review focus:** %s

            **Provide your review in Markdown format:**

            ## 📊 Code Quality Score
            Rate the code out of 10 in these categories:
            | Category | Score | Comment |
            |----------|-------|---------|
            | Readability | /10 | brief note |
            | Performance | /10 | brief note |
            | Best Practices | /10 | brief note |
            | Error Handling | /10 | brief note |
            | **Overall** | **/10** | summary |

            ## ✅ What's Done Well
            List positive aspects of the code (at least 2-3 points).

            ## ⚠️ Issues Found
            For each issue:
            - **Issue**: Description
            - **Location**: Where in the code
            - **Impact**: Why it matters
            - **Fix**: How to resolve it with code example

            ## 🚀 Improvement Suggestions
            Suggested improvements with refactored code examples.

            ## 🔒 Security Considerations
            Any security concerns (if applicable).

            ## 📌 Summary
            A brief overall assessment with top 3 action items.

            Be constructive, specific, and educational. Show improved code where relevant.
            """,
                request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java",
                request.getMessage() != null ? request.getMessage() : "// No code provided",
                focus
        );
    }

    private String buildPracticeExercisePrompt(AiRequest request) {
        int count = request.getQuestionCount() != null ? request.getQuestionCount() : 3;
        return String.format("""
            You are an expert programming instructor on the "Code Arena" platform.
            Generate %d practice coding exercises for a student.

            **Exercise Parameters:**
            - Topic: %s
            - Programming Language: %s
            - Difficulty Level: %s

            **For each exercise, provide in Markdown format:**

            ## 🏋️ Exercise [number]: [Catchy Title]

            ### 📋 Problem Statement
            A clear description of what the student needs to build/solve.

            ### 🎯 Requirements
            - Numbered list of specific requirements
            - Include input/output examples

            ### 💡 Hints
            2-3 hints to guide the student (without giving away the solution).

            ### ✅ Solution
            ```%s
            // Complete working solution with comments
            ```

            ### 🧠 Explanation
            Explain the solution approach and why it works.

            ### 🌟 Bonus Challenge
            An optional harder variation of the exercise.

            ---

            Make exercises progressively harder.
            Ensure all code is syntactically correct and well-commented.
            Use real-world scenarios when possible to make exercises engaging.
            """,
                count,
                request.getTopic() != null ? request.getTopic() : "General Programming",
                request.getLanguage() != null ? request.getLanguage() : "JAVA",
                request.getLevel() != null ? request.getLevel() : "INTERMEDIAIRE",
                request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java"
        );
    }

    private String buildDebugHelpPrompt(AiRequest request) {
        String verbosity = request.getLevel() != null ? request.getLevel() : "detailed";
        String errorMsg = request.getContext() != null ? request.getContext() : "No error message provided";
        return String.format("""
            You are an expert debugger and programming mentor on the "Code Arena" platform.
            A student needs help finding and fixing bugs in their code.

            **Buggy Code:**
            ```%s
            %s
            ```

            **Error/Problem Description:** %s

            **Response verbosity:** %s

            **Provide your debug analysis in Markdown format:**

            ## 🐛 Bug Report

            ### 🔍 Root Cause Analysis
            Identify the bug(s) and explain why they occur.

            ### 📍 Bug Location
            Point to the exact line(s) where bugs exist.

            ### ✅ Fixed Code
            ```%s
            // Complete corrected code with fix annotations
            ```

            ### 📝 What Changed & Why
            For each fix:
            - **Before**: the buggy code
            - **After**: the fixed code
            - **Why**: explanation of the fix

            ### 🛡️ Prevention Tips
            How to avoid similar bugs in the future.

            ### 🧪 How to Test
            Suggest test cases to verify the fix works.

            Adjust detail level based on verbosity:
            - quick: Just show the fix with minimal explanation
            - detailed: Explain the bug and fix thoroughly
            - educational: Deep dive into why the bug happened and related concepts
            """,
                request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java",
                request.getMessage() != null ? request.getMessage() : "// No code provided",
                errorMsg,
                verbosity,
                request.getLanguage() != null ? request.getLanguage().toLowerCase() : "java"
        );
    }

    @SuppressWarnings("unchecked")
    private String callHuggingFace(String prompt) {
        // Check if API key is configured
        if (huggingFaceApiKey == null || huggingFaceApiKey.isBlank()) {
            log.warn("Hugging Face API key not configured, using fallback response");
            return generateFallbackResponse(prompt);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(huggingFaceApiKey);

            // Build OpenAI-compatible chat completions request body
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", HF_MODEL);
            body.put("messages", List.of(userMessage));
            body.put("max_tokens", 2048);
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Calling Hugging Face Router API with model: {}", HF_MODEL);
            ResponseEntity<Map> response = restTemplate.postForEntity(HF_CHAT_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // OpenAI-compatible format: { choices: [{ message: { content: "..." } }] }
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, Object> messageResp = (Map<String, Object>) firstChoice.get("message");
                        if (messageResp != null && messageResp.containsKey("content")) {
                            String content = (String) messageResp.get("content");
                            return content != null ? content.trim() : "No content generated.";
                        }
                    }
                }

                // Handle error in response body
                if (responseBody.containsKey("error")) {
                    String error = String.valueOf(responseBody.get("error"));
                    log.error("Hugging Face API error: {}", error);
                    if (error.toLowerCase().contains("loading") || error.toLowerCase().contains("currently loading")) {
                        return "⏳ Le modèle est en cours de chargement sur Hugging Face. Veuillez réessayer dans 20-30 secondes.";
                    }
                    return "❌ Erreur Hugging Face: " + error;
                }

                return "❌ ERREUR FORMAT: Réponse inattendue: " + responseBody.toString();
            }

            return "❌ ERREUR API (Status " + response.getStatusCode() + ") : La réponse est vide ou mal formatée.";
        } catch (Exception e) {
            log.error("Hugging Face API call failed: {}", e.getMessage());
            e.printStackTrace();

            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";

            // Model loading (503)
            if (errorMsg.contains("503")) {
                return "⏳ Le modèle Hugging Face est en cours de chargement. Veuillez réessayer dans 20-30 secondes.";
            }
            // Rate limit (429)
            if (errorMsg.contains("429")) {
                return "⚠️ Limite de requêtes atteinte. Veuillez réessayer dans quelques minutes.";
            }
            // Auth error (401/403)
            if (errorMsg.contains("401") || errorMsg.contains("403")) {
                return "🔑 Clé API Hugging Face invalide ou expirée. Vérifiez votre token dans le fichier .env.";
            }

            return "❌ ERREUR API HUGGING FACE : La requête a échoué.\nDétails: " + errorMsg;
        }
    }

    private String generateFallbackResponse(String prompt) {
        // Intelligent fallback when no API key is configured
        if (prompt.contains("SESSION_PLAN") || prompt.contains("Session Agenda")) {
            return """
                ## 🎯 AI-Generated Session Blueprint

                ## 📋 Learning Objectives
                1. Understand core concepts and practical applications
                2. Build hands-on skills through guided exercises
                3. Develop problem-solving strategies
                4. Apply best practices in real-world scenarios

                ## 📅 Session Agenda
                | Time | Activity | Description |
                |------|---------|-------------|
                | 0-10 min | **Ice Breaker** | Quick coding challenge to warm up |
                | 10-25 min | **Core Concepts** | Theory with live coding demonstrations |
                | 25-45 min | **Hands-on Lab** | Guided coding exercises |
                | 45-55 min | **Code Review** | Peer review and best practices |
                | 55-60 min | **Q&A & Wrap-up** | Questions and homework assignment |

                ## 💻 Code Exercises
                ### Exercise 1: Foundation Builder
                Start with basic concepts and build incrementally.

                ### Exercise 2: Pattern Recognition
                Identify and implement common design patterns.

                ### Exercise 3: Challenge Mode
                Solve a real-world problem using learned concepts.

                ## 💡 Discussion Points
                - What are the trade-offs of different approaches?
                - How does this apply to production code?
                - What are common mistakes to avoid?

                ## 📝 Homework
                1. Refactor the exercise code using best practices
                2. Build a mini project applying today's concepts
                3. Write unit tests for your implementation

                ## 🏆 Success Criteria
                - Learners can independently write solutions
                - Understanding of core concepts > 80%
                - Active participation in code reviews

                > 💡 **Tip:** Configure your Hugging Face API key in .env for AI-powered personalized plans!
                """;
        }

        if (prompt.contains("QUIZ") || prompt.contains("quiz questions")) {
            return """
                {
                  "quizTitle": "Programming Fundamentals Quiz",
                  "quizDescription": "Test your understanding of core programming concepts",
                  "questions": [
                    {
                      "content": "What is the time complexity of binary search?",
                      "type": "MCQ",
                      "options": "O(n)|||O(log n)|||O(n²)|||O(1)",
                      "correctAnswer": "O(log n)",
                      "explanation": "Binary search divides the search space in half at each step, resulting in logarithmic time complexity.",
                      "codeSnippet": null,
                      "points": 10
                    },
                    {
                      "content": "What will be the output of this code?",
                      "type": "CODE_ANALYSIS",
                      "options": "5|||10|||15|||Error",
                      "correctAnswer": "10",
                      "explanation": "The variable is reassigned before being printed.",
                      "codeSnippet": "int x = 5;\\\\nx = x * 2;\\\\nSystem.out.println(x);",
                      "points": 15
                    },
                    {
                      "content": "Complete the function to reverse a string:",
                      "type": "CODE_COMPLETION",
                      "options": "charAt(i)|||substring(i)|||toCharArray()|||reverse()",
                      "correctAnswer": "charAt(i)",
                      "explanation": "Using charAt(i) with a loop from end to start builds the reversed string.",
                      "codeSnippet": "String reverse(String s) {\\\\n  String result = \\\\\\"\\\\\\";\\\\n  for(int i = s.length()-1; i >= 0; i--)\\\\n    result += s._____; \\\\n  return result;\\\\n}",
                      "points": 20
                    }
                  ]
                }
                """;
        }

        // Chat fallback
        return """
            ## 🤖 ARIA - AI Coaching Assistant

            Hello, Coach! I'm ARIA, your AI-powered coaching companion.

            Here's how I can help you today:

            ### 📚 Teaching Strategies
            - **Active Learning**: Engage students with pair programming and live coding exercises
            - **Scaffolding**: Break complex topics into digestible chunks
            - **Formative Assessment**: Use quick quizzes to check understanding

            ### 💡 Session Tips
            1. Start with a **warm-up challenge** (5 min)
            2. Use the **I Do, We Do, You Do** method
            3. End with a **reflection exercise**

            ### 🎯 Best Practices
            - Keep code examples **short and focused**
            - Use **real-world analogies** for abstract concepts
            - Encourage **questions** throughout the session

            > 💡 **Note:** Configure your Hugging Face API key in .env for fully personalized AI responses!

            How can I help you prepare for your next session?
            """;
    }
}
