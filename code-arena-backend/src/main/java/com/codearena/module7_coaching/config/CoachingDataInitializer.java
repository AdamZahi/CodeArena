package com.codearena.module7_coaching.config;

import com.codearena.module7_coaching.entity.*;
import com.codearena.module7_coaching.enums.*;
import com.codearena.module7_coaching.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class CoachingDataInitializer implements CommandLineRunner {

        private final QuizRepository quizRepository;
        private final QuestionRepository questionRepository;
        private final CoachingSessionRepository sessionRepository;
        private final CoachingBadgeRepository badgeRepository;

        @Override
        public void run(String... args) {
                // Force refresh for new requirements
                // questionRepository.deleteAll();
                // quizRepository.deleteAll();

                log.info("Initializing coaching module seed data...");

                initBadges();

                // Prevent infinite duplication on restart
                if (quizRepository.findByTitle("Multi-Language Problem Solving Challenge").isEmpty()) {
                        initQuizWithQuestions();
                        initQuizzes();
                } else {
                        log.info("System quizzes already exist, skipping quiz initialization.");
                }

                initSessions();

                log.info("Coaching module seed data initialized successfully!");
        }

        private void initBadges() {
                if (badgeRepository.count() > 0)
                        return;
                List.of(
                                CoachingBadge.builder().name("Premier Quiz").description("Completed your first quiz")
                                                .build(),
                                CoachingBadge.builder().name("Premier Coaching")
                                                .description("Booked your first coaching session")
                                                .build(),
                                CoachingBadge.builder().name("Expert Java")
                                                .description("Achieved advanced level in Java").build(),
                                CoachingBadge.builder().name("Expert Python")
                                                .description("Achieved advanced level in Python").build(),
                                CoachingBadge.builder().name("Polyglotte")
                                                .description("Scored above 70% in 3+ languages").build(),
                                CoachingBadge.builder().name("Quiz Master").description("Completed 10 quizzes").build())
                                .forEach(badgeRepository::save);
        }

        private void initQuizWithQuestions() {
                // 1. Multi-language Problem Solving (10 x 10pts)
                Quiz quiz = quizRepository.save(Quiz.builder().title("Multi-Language Problem Solving Challenge")
                                .description("Test your diagnostic skills across Java, Python, and JavaScript.")
                                .difficulty(QuizDifficulty.MEDIUM).language(ProgrammingLanguage.MULTI)
                                .category("PROBLEM_SOLVING").createdBy("system").totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("Java: Output of '1 + 2 + \"3\"'?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("33").options("6,33,123,error").build());
                questionRepository.save(Question.builder().quizId(quiz.getId()).content("JS: 'typeof []' returns?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("object")
                                .options("array,object,list,undefined").build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("Python: How to get slice excluding last 2?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("list[:-2]").options("list[:2],list[-2:],list[:-2],list.slice(-2)")
                                .build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("Java: Which is checked exception?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("IOException")
                                .options("NullPointerException,IOException,ArithmeticException,ClassCastException")
                                .build());
                questionRepository.save(Question.builder().quizId(quiz.getId()).content("JS: '5' + 5 result?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("55")
                                .options("10,55,NaN,error").build());
                questionRepository.save(Question.builder().quizId(quiz.getId()).content("Python: Which is immutable?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("tuple")
                                .options("list,set,dict,tuple").build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("CSS: Which hides element but keeps space?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.CSS).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("visibility: hidden")
                                .options("display: none,opacity: 0,visibility: hidden,position: absolute").build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("Angular: How to listen to events?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.ANGULAR).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("(click)").options("[click],{click},(click),*click").build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content(".NET: Interface for async tasks?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.DOTNET).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("Task").options("Runnable,Thread,Task,Async").build());
                questionRepository.save(Question.builder().quizId(quiz.getId())
                                .content("SQL: Which keyword to remove duplicates?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("DISTINCT").options("UNIQUE,DISTINCT,REMOVE,SINGLE").build());

                // 2. Java Advanced Patterns & Algorithms (10 x 10pts)
                Quiz javaQuiz = quizRepository.save(Quiz.builder().title("Java Advanced Patterns & Algorithms")
                                .description("Deep Java concurrency and memory model concepts.")
                                .difficulty(QuizDifficulty.HARD).language(ProgrammingLanguage.JAVA)
                                .category("PROBLEM_SOLVING").createdBy("system").totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("What is 'Double-Checked Locking' used for?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Lazy Initialization")
                                .options("Caching,Lazy Initialization,Serialization,Polymorphism").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("Which GC uses 'Stop-the-world' least?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("ZGC").options("Serial,Parallel,G1,ZGC").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("ConcurrentHashMap bucket locking type?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Segment/Node locking")
                                .options("Object locking,Read locking,Segment/Node locking,Global locking").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId()).content("What does ThreadLocal do?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVA)
                                .difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Variables private to thread")
                                .options("Static variables,Variables private to thread,Shared variables,Global variables")
                                .build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("Java 8: Default method purpose?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Backward compatibility")
                                .options("Performance,Backward compatibility,Security,Memory management").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("Optional.ofNullable(null).orElse(\"A\") returns?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("A").options("null,A,error,RuntimeException").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("CountDownLatch 'await' waits until?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Count reaches zero")
                                .options("Thread finishes,Count reaches total,Count reaches zero,Exception occurs")
                                .build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId()).content("Phaser vs CyclicBarrier?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVA)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Phaser is dynamic")
                                .options("Phaser is slower,CyclicBarrier is dynamic,Phaser is dynamic,No difference")
                                .build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("Java Memory Model: Stack stores?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Local variables")
                                .options("Objects,Static vars,Local variables,Methods only").build());
                questionRepository.save(Question.builder().quizId(javaQuiz.getId())
                                .content("Reference type for Caching?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVA).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("SoftReference")
                                .options("WeakReference,SoftReference,PhantomReference,StrongReference").build());

                // 3. Python Mastery (10 x 10pts)
                Quiz pyQuiz = quizRepository.save(Quiz.builder().title("Python Data & Algorithmic Kata")
                                .description("Python internals and data structure efficiency.")
                                .difficulty(QuizDifficulty.MEDIUM).language(ProgrammingLanguage.PYTHON)
                                .category("PROBLEM_SOLVING").createdBy("system").totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId()).content("Python: Purpose of GIL?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Ensure thread safety for CPython objects")
                                .options("Speed up loops,Security,Ensure thread safety for CPython objects,Memory cleanup")
                                .build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId())
                                .content("Decorator '@classmethod' first arg?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("cls").options("self,class,cls,instance").build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId())
                                .content("Which method makes class instance callable?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("__call__").options("__init__,__call__,__exec__,__run__").build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId())
                                .content("Difference between list.sort() and sorted()?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("list.sort() is in-place")
                                .options("sorted() is in-place,list.sort() is in-place,no difference,list.sort() is for tuples")
                                .build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId()).content("What is a generator?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Function yielding multiple values")
                                .options("Class with loops,Function yielding multiple values,Global variable,Optimizer")
                                .build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId()).content("Complexity of 'x in set'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("O(1)")
                                .options("O(n),O(log n),O(1),O(n^2)").build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId()).content("What is 'monkey patching'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Modifying code at runtime")
                                .options("Modifying files,Modifying code at runtime,Fixing hardware,Security update")
                                .build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId())
                                .content("Keyword for Context Managers?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("with").options("open,with,try,context").build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId())
                                .content("How to copy a list completely?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.PYTHON).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("copy.deepcopy()")
                                .options("list.copy(),list[:],copy.deepcopy(),new = list").build());
                questionRepository.save(Question.builder().quizId(pyQuiz.getId()).content("Python: Result of bool([])?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.PYTHON)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("False")
                                .options("True,False,Error,None").build());
        }

        private void initQuizzes() {
                // 4. Docker & DevOps (10 x 10pts)
                Quiz dQuiz = quizRepository.save(Quiz.builder().title("Docker & CI/CD Masterclass")
                                .description("Master containers and CI/CD pipelines.").difficulty(QuizDifficulty.MEDIUM)
                                .language(ProgrammingLanguage.MULTI).category("DEVOPS").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("Docker: Diff between Image and Container?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("Container is running Image")
                                .options("Image is running Container,Container is running Image,They are same,No idea")
                                .build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId()).content("Command to see running logs?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("docker logs")
                                .options("docker ps,docker run,docker logs,docker view").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId()).content("What is a Multi-stage build?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Optimizing image size")
                                .options("Building 2 apps,Optimizing image size,Running 2 ports,Cloud build").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("Kubernetes: Purpose of a Pod?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Smallest deployable unit")
                                .options("Storage unit,Smallest deployable unit,Network bridge,Monitor").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("CI/CD: Goal of 'Continuous Deployment'?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Auto release to prod")
                                .options("Auto build,Write tests,Auto release to prod,Manual approval").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("Docker: File to define services?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("docker-compose.yml")
                                .options("Dockerfile,config.yml,docker-compose.yml,services.json").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("What keyword in Dockerfile adds files?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("COPY").options("GET,PULL,COPY,MOVE").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("DevOps: Infrastructure as Code tool?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Terraform").options("Docker,Git,Terraform,Jenkins").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("Docker: Default network type?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("bridge").options("host,none,bridge,overlay").build());
                questionRepository.save(Question.builder().quizId(dQuiz.getId())
                                .content("Kubernetes: Command to get nodes?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("kubectl get nodes")
                                .options("kube nodes,kubectl show nodes,kubectl get nodes,kubectl status").build());

                // 5. SQL & Database Optimization (10 x 10pts)
                Quiz sQuiz = quizRepository.save(Quiz.builder().title("SQL & Database Optimization")
                                .description("Advanced queries and indexing.").difficulty(QuizDifficulty.HARD)
                                .language(ProgrammingLanguage.MULTI).category("DATABASES").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId())
                                .content("SQL: Difference between UNION and UNION ALL?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("UNION removes duplicates")
                                .options("UNION ALL removes duplicates,UNION removes duplicates,No difference,UNION is for counts")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("What is a 'Full Outer Join'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Returns all records when match occurs")
                                .options("Only matches,Only left side,Returns all records when match occurs,Internal join")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("DB: Database Index cost?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Slower INSERT/UPDATE")
                                .options("Slower SELECT,Slower INSERT/UPDATE,Larger UI,No cost").build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("What is the 'N+1' Problem?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Excessive DB queries in loops")
                                .options("Memory leak,Excessive DB queries in loops,Server timeout,SQL Injection")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("SQL: PRIMARY KEY vs UNIQUE?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("PK cannot be NULL")
                                .options("Unique cannot be NULL,PK cannot be NULL,PK is indexed Unique is not,None")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("What means ACID?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Consistency, Isolation...")
                                .options("Speed, Security...,Consistency, Isolation...,Backup, Log...,Reliability")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("SQL: Order of execution?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("FROM -> WHERE -> SELECT")
                                .options("SELECT -> FROM -> WHERE,FROM -> WHERE -> SELECT,WHERE -> FROM -> SELECT,Same as written")
                                .build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("What is a View?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Virtual table results")
                                .options("Physical table,Virtual table results,Index backup,UI layout").build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("NoSQL: Key characteristic?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Schema-less")
                                .options("Strict schema,Schema-less,Only for strings,Relational only").build());
                questionRepository.save(Question.builder().quizId(sQuiz.getId()).content("SQL: Purpose of TRUNCATE?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Remove all data instantly")
                                .options("Delete specific rows,Remove all data instantly,Back up table,Alter structure")
                                .build());

                // 6. React Modern Patterns (10 x 10pts)
                Quiz rQuiz = quizRepository.save(Quiz.builder().title("React Modern Patterns")
                                .description("Hooks and state management.").difficulty(QuizDifficulty.MEDIUM)
                                .language(ProgrammingLanguage.JAVASCRIPT).category("FRAMEWORKS").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("React: Hook for state?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("useState")
                                .options("useEffect,useContext,useState,useReducer").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId())
                                .content("React: Life cycle equivalent of ComponentDidMount?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVASCRIPT).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("useEffect(() => {}, [])")
                                .options("useEffect(() => {}),useEffect(() => {}, []),useSync,useState").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("React: What is JSX?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Syntax extension for JS")
                                .options("CSS variant,Logic tool,Syntax extension for JS,Database handler").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId())
                                .content("What does 'lifting state up' mean?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVASCRIPT).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Moving state to common parent")
                                .options("Global state,Moving state to common parent,Deleting state,Moving state to DB")
                                .build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("What is a Pure Component?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Re-renders only if props change")
                                .options("Sync component,Re-renders only if props change,Styleless component,Empty component")
                                .build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId())
                                .content("React: purpose of 'key' prop in lists?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.JAVASCRIPT).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("Identity for Reconciliation")
                                .options("Sorting,Styling,Identity for Reconciliation,Database sync").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("Redux: Main building blocks?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Action, Reducer, Store")
                                .options("HTML, CSS, JS,Action, Reducer, Store,Node, Express, Mongo,State, Prop, Ref")
                                .build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("What is React Fiber?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Reconciliation engine")
                                .options("New CSS set,Reconciliation engine,Cloud service,Backend framework").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("React: useMemo purpose?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Performance optimization")
                                .options("Managing forms,Performance optimization,Server calls,Routing").build());
                questionRepository.save(Question.builder().quizId(rQuiz.getId()).content("What is HOC?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.JAVASCRIPT)
                                .difficulty(QuizDifficulty.HARD).points(10).correctAnswer("Higher Order Component")
                                .options("High Object Code,Higher Order Component,Heavy Object Class,none").build());

                // 7. Cybersecurity Fundamentals (10 x 10pts)
                Quiz cQuiz = quizRepository.save(Quiz.builder().title("Cybersecurity Fundamentals")
                                .description("Web security and encryption.").difficulty(QuizDifficulty.EASY)
                                .language(ProgrammingLanguage.MULTI).category("SECURITY").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("What is 'Phishing'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Fraudulent emails")
                                .options("Network virus,Fraudulent emails,Server crash,Encryption type").build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("Security: Role of 2FA?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Double authentication")
                                .options("Double files,Double authentication,Fast access,Backup").build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("What is 'Ransomware'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Malware demanding payment")
                                .options("Hardware tool,Malware demanding payment,Keylogger,Browser extension")
                                .build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("What does HTTPS provide?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Encryption/Integrity")
                                .options("Faster speed,Better SEO only,Encryption/Integrity,More images").build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId())
                                .content("What is a 'Brute Force' attack?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Trying every combination")
                                .options("Physical damage,Trying every combination,SQL call,Social engineering")
                                .build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("What is a 'Firewall'?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Network security system")
                                .options("OS backup,Network security system,Antivirus,Router").build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("OWASP Top 10 purpose?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.MEDIUM).points(10).correctAnswer("Guide of top web risks")
                                .options("Java patterns list,Guide of top web risks,SQL commands list,UI best practices")
                                .build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId())
                                .content("What is 'Zero-Day' vulnerability?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.HARD).points(10)
                                .correctAnswer("Unknown to vendor")
                                .options("Vulnerability on day 0,Unknown to vendor,Fixed vulnerability,Server down")
                                .build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId()).content("Purpose of VPN?")
                                .type(QuestionType.MCQ).language(ProgrammingLanguage.MULTI)
                                .difficulty(QuizDifficulty.EASY).points(10).correctAnswer("Secure/Private tunnel")
                                .options("Increase RAM,Secure/Private tunnel,Download movies,Sync calendar").build());
                questionRepository.save(Question.builder().quizId(cQuiz.getId())
                                .content("What is 'Salting' in hashing?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.MULTI).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Adding random data to pass")
                                .options("Adding random data to pass,Compressing hash,Deleting hash,none").build());
                // 8. Angular Mastery (10 x 10pts)
                Quiz aQuiz = quizRepository.save(Quiz.builder().title("Angular RxJS & Components")
                                .description("Master Angular core features and reactive programming.")
                                .difficulty(QuizDifficulty.HARD)
                                .language(ProgrammingLanguage.ANGULAR).category("FRAMEWORKS").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(aQuiz.getId())
                                .content("What is RxJS mostly used for in Angular?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.ANGULAR).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Handling asynchronous operations")
                                .options("Styling,Handling asynchronous operations,Database connection,Routing").build());
                questionRepository.save(Question.builder().quizId(aQuiz.getId())
                                .content("Which decorator is used to define a component?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.ANGULAR).difficulty(QuizDifficulty.EASY).points(10)
                                .correctAnswer("@Component")
                                .options("@Injectable,@Module,@Component,@Directive").build());

                // 9. .NET Core Fundamentals (10 x 10pts)
                Quiz dnQuiz = quizRepository.save(Quiz.builder().title(".NET Core Microservices")
                                .description("Build robust APIs with C# and .NET Core.")
                                .difficulty(QuizDifficulty.MEDIUM)
                                .language(ProgrammingLanguage.DOTNET).category("BACKEND").createdBy("system")
                                .totalPoints(100).build());
                questionRepository.save(Question.builder().quizId(dnQuiz.getId())
                                .content("What is Entity Framework Core?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.DOTNET).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("An ORM framework")
                                .options("A UI library,An ORM framework,A testing tool,A cloud provider").build());
                questionRepository.save(Question.builder().quizId(dnQuiz.getId())
                                .content("Which file contains dependency injection configuration by default?").type(QuestionType.MCQ)
                                .language(ProgrammingLanguage.DOTNET).difficulty(QuizDifficulty.MEDIUM).points(10)
                                .correctAnswer("Program.cs")
                                .options("appsettings.json,Startup.cs,Program.cs,web.config").build());
        }

        private void initSessions() {
                if (sessionRepository.count() >= 8)
                        return;
                LocalDateTime now = LocalDateTime.now();

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-java-001").title("Java Spring Boot Fundamentals")
                                .description(
                                                "Learn the core concepts of Spring Boot: dependency injection, auto-configuration, RESTful APIs, and data access with JPA.")
                                .language(ProgrammingLanguage.JAVA).level(SkillLevel.BASIQUE)
                                .scheduledAt(now.plusDays(3)).durationMinutes(90)
                                .maxParticipants(15).meetingUrl("https://meet.codearena.com/java-basics")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-java-001").title("Java Microservices Architecture")
                                .description(
                                                "Advanced session on microservices patterns: API Gateway, Service Discovery, Circuit Breaker, and Event-Driven Architecture.")
                                .language(ProgrammingLanguage.JAVA).level(SkillLevel.AVANCE)
                                .scheduledAt(now.plusDays(5)).durationMinutes(120)
                                .maxParticipants(10).meetingUrl("https://meet.codearena.com/java-micro")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-python-001").title("Python for Data Science")
                                .description("Introduction to NumPy, Pandas, and Matplotlib for data analysis and visualization.")
                                .language(ProgrammingLanguage.PYTHON).level(SkillLevel.BASIQUE)
                                .scheduledAt(now.plusDays(2)).durationMinutes(90)
                                .maxParticipants(20).meetingUrl("https://meet.codearena.com/python-ds")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-python-001").title("Python Advanced Algorithms")
                                .description(
                                                "Deep dive into dynamic programming, graph algorithms, and optimization techniques in Python.")
                                .language(ProgrammingLanguage.PYTHON).level(SkillLevel.INTERMEDIAIRE)
                                .scheduledAt(now.plusDays(7)).durationMinutes(90)
                                .maxParticipants(12).meetingUrl("https://meet.codearena.com/python-algo")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-angular-001").title("Angular Reactive Forms & RxJS")
                                .description("Master reactive forms, custom validators, and RxJS operators for building dynamic UIs.")
                                .language(ProgrammingLanguage.ANGULAR).level(SkillLevel.INTERMEDIAIRE)
                                .scheduledAt(now.plusDays(4)).durationMinutes(90)
                                .maxParticipants(15).meetingUrl("https://meet.codearena.com/angular-rxjs")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-angular-001").title("JavaScript ES6+ Mastery")
                                .description(
                                                "Comprehensive coverage of modern JavaScript: destructuring, modules, generators, Proxy, and async patterns.")
                                .language(ProgrammingLanguage.JAVASCRIPT).level(SkillLevel.BASIQUE)
                                .scheduledAt(now.plusDays(6)).durationMinutes(60)
                                .maxParticipants(20).meetingUrl("https://meet.codearena.com/js-es6")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-angular-001").title("CSS Grid & Flexbox Masterclass")
                                .description(
                                                "Learn modern CSS layout techniques with Grid and Flexbox. Build responsive layouts from scratch.")
                                .language(ProgrammingLanguage.CSS).level(SkillLevel.BASIQUE)
                                .scheduledAt(now.plusDays(8)).durationMinutes(60)
                                .maxParticipants(25).meetingUrl("https://meet.codearena.com/css-layouts")
                                .build());

                sessionRepository.save(CoachingSession.builder()
                                .coachId("coach-java-001").title(".NET Core Web API Development")
                                .description(
                                                "Build RESTful APIs with .NET Core: controllers, middleware, Entity Framework, and authentication.")
                                .language(ProgrammingLanguage.DOTNET).level(SkillLevel.INTERMEDIAIRE)
                                .scheduledAt(now.plusDays(9)).durationMinutes(90)
                                .maxParticipants(12).meetingUrl("https://meet.codearena.com/dotnet-api")
                                .build());
        }
}
