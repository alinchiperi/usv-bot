package ro.usv.usv.bot.advisor;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class UsvQueryAugmenter implements QueryAugmenter {
    private static final Logger log = LoggerFactory.getLogger(UsvQueryAugmenter.class);
    private static final PromptTemplate PROMPT_TEMPLATE
            = new PromptTemplate("""
            Esti un consultat pentru Universitatea "Stefan cel Mare" din Suceava (USV).
            Vei raspunde politicos si profesionalist folosind contextul.
            Contextul este mai jos, inconjurat de ---------
            ---------------------
            {context}
            ---------------------
            Având în vedere informațiile contextuale și fără cunoștințe anterioare, răspundeți la intrebare
            Urmați aceste reguli::
            1. Dacă răspunsul nu este în context, spuneți pur și simplu ca nu aveti informatii.
            2. Evitați afirmații precum „Pe baza contextului...” sau „Informațiile furnizate..."
            Interogare: {query}
            Raspuns:""");

    private static final PromptTemplate EMPTY_CONTEXT_PROMPT_TEMPLATE
            = new PromptTemplate("""
            Interogarea utilizatorului este în afara bazei ta de cunoștințe.
            Informați politicos utilizatorul că nu poti răspunde la ea fara sa specifici contextul.
            intrebarea: {query}""");


    @NonNull
    public Query augment(Query query, List<Document> documents) {
        log.debug("Augmenting query with contextual data, query: {}", query.text());
        if (documents.isEmpty()) {
            return augmentQueryWhenEmptyContext(query);
        } else {
            return augmentQuery(query, documents);
        }
    }

    private Query augmentQuery(Query query, List<Document> documents) {
        String documentContext = documents.
                stream()
                .map(Document::getText).
                collect(Collectors.joining(System.lineSeparator()));
        log.info("Augment context: {}", documentContext);
        Map<String, Object> promptParameters = Map.of("query", query.text(),
                "context", documentContext);
        return new Query(PROMPT_TEMPLATE.render(promptParameters));
    }


    private Query augmentQueryWhenEmptyContext(Query query) {
        Map<String, Object> promptParameters = Map.of("query", query.text());
        return new Query(EMPTY_CONTEXT_PROMPT_TEMPLATE.render(promptParameters));
    }
}