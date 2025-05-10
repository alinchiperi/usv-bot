package ro.usv.usv.bot.advisor;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Builder
public class UsvAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";
    private static final String USER_ADVICE = """
            Esti un consultat pentru Universitatea "Stefan cel Mare" din Suceava (USV).
            Vei raspunde politicos si profesionalist folosind contextul.
            Informatiile de context sunt mai jos, inconjurate de
           \s
            ---------------------
            {question_answer_context}
            ---------------------
            Avand în vedere contextul si informatiile istorice furnizate, si nu cunostintele anterioare,
            raspunde la comentariul utilizatorului. Daca raspunsul nu se afla în context, informeaza
            utilizatorul ca nu poti raspunde la întrebare.
            In raspuns, nu vei spune ca ai informatii furnizate.
            Vei da raspunsul in limba romana!""";

    private static final Logger log = LoggerFactory.getLogger(UsvAdvisor.class);
    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final String userTextAdvise;
    private static final int DEFAULT_ORDER = 0;


    public UsvAdvisor(VectorStore vectorStore, SearchRequest searchRequest, String userTextAdvise) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.userTextAdvise = userTextAdvise;
    }

    public UsvAdvisor( VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.userTextAdvise = USER_ADVICE;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        AdvisedRequest advisedRequest2 = this.before(advisedRequest);
        log.info("Advised request aroundCall: {}", advisedRequest2);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest2);
        log.info("Advised response: {}", advisedResponse);
        return this.after(advisedResponse);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(before(advisedRequest));

        return advisedResponses.map(ar -> {
            if (onFinishReason().test(ar)) {
                ar = after(ar);
            }
            return ar;
        });
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("Before advice for request: {}", request);

        var context = new HashMap<>(request.adviseContext());

        // 1. Advise the system text.
        String advisedUserText = request.userText() + System.lineSeparator() + this.userTextAdvise;
        log.info("Advised user text: {}", advisedUserText);

        // 2. Search for similar documents in the vector store.
        String query = new PromptTemplate(request.userText(), request.userParams()).render();
        log.info("Query: {}", query);

        var searchRequestToUse = SearchRequest.from(this.searchRequest)
                .query(query)
                .build();
        log.info("Search request: {}", searchRequestToUse);

        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
        log.info("Found documents: {}", documents.size());

        context.put(RETRIEVED_DOCUMENTS, documents);
        log.info("Context: {}", context);

        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
        advisedUserParams.put("question_answer_context", documentContext);

        AdvisedRequest advisedRequest = AdvisedRequest.from(request)
                .userText(advisedUserText)
                .userParams(advisedUserParams)
                .adviseContext(context)
                .build();
        log.info("Advised request: {}", advisedRequest);

        return advisedRequest;
    }

    private AdvisedResponse after(AdvisedResponse advisedResponse) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(advisedResponse.response());
        log.info("After advice for response: {}", advisedResponse);
        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, advisedResponse.adviseContext().get(RETRIEVED_DOCUMENTS));
        return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
    }


    private Predicate<AdvisedResponse> onFinishReason() {
        return advisedResponse -> advisedResponse.response()
                .getResults()
                .stream()
                .anyMatch(result -> result != null && result.getMetadata() != null
                        && StringUtils.hasText(result.getMetadata().getFinishReason()));
    }

}
