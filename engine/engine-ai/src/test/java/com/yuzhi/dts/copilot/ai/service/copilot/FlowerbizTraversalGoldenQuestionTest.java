package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.DataLayer;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlowerbizTraversalGoldenQuestionTest {

    private static final double MIN_HIT_RATE = 0.9;

    @Mock
    private IntentRouterService intentRouterService;

    @Mock
    private TemplateMatcherService templateMatcherService;

    @Mock
    private BusinessDirectResponseCatalogService directResponseCatalogService;

    private AssetBackedPlannerPolicy policy;

    @BeforeEach
    void setUp() {
        SemanticPackService semanticPackService = new SemanticPackService(new ObjectMapper());
        semanticPackService.init();
        policy = new AssetBackedPlannerPolicy(
                intentRouterService,
                templateMatcherService,
                semanticPackService,
                new OntologyService(semanticPackService),
                directResponseCatalogService,
                new AgentBiReportCatalogService(),
                new BusinessObjectCatalogService()
        );
    }

    @Test
    void flowerbizTraversalGoldenQuestionsHitObjectGraphNavigationAtLeast90Percent() throws IOException {
        List<GoldenQuestion> questions = loadGoldenQuestions();
        List<String> misses = new ArrayList<>();

        for (GoldenQuestion golden : questions) {
            ConversationPlan plan = plan(golden);
            if (!isHit(golden, plan)) {
                misses.add(golden.id() + " -> kind=" + plan.responseKind()
                        + ", dataSurface=" + plan.dataSurface()
                        + ", refs=" + plan.sourceRefs()
                        + ", expectedPath=" + golden.expectedPath());
            }
        }

        double hitRate = (questions.size() - misses.size()) / (double) questions.size();
        assertThat(hitRate)
                .as("traversal golden hit rate, misses: %s", misses)
                .isGreaterThanOrEqualTo(MIN_HIT_RATE);
    }

    private ConversationPlan plan(GoldenQuestion golden) {
        when(templateMatcherService.match(golden.question()))
                .thenReturn(new TemplateMatchResult(false, null, null, null));
        when(intentRouterService.routeWithDataLayer(golden.question(), Map.of()))
                .thenReturn(new ExtendedRoutingResult(
                        new RoutingResult(golden.domain(), "public.xycyl_ads_flowerbiz_lease_detail",
                                List.of(), 0.86, false),
                        DataLayer.VIEW,
                        null,
                        false,
                        null));
        when(directResponseCatalogService.findMatch(golden.question())).thenReturn(Optional.empty());

        return policy.plan(golden.question(), Map.of());
    }

    private boolean isHit(GoldenQuestion golden, ConversationPlan plan) {
        if (plan.responseKind() != ResponseKind.OBJECT_GRAPH_NAVIGATION) {
            return false;
        }
        if (!"L1_ONTOLOGY_GRAPH".equals(plan.dataSurface())) {
            return false;
        }
        if (!plan.sourceRefs().containsAll(golden.expectedSourceRefs())) {
            return false;
        }
        if (!plan.promptContext().contains("link path: " + golden.expectedPath().replace(">", " -> "))) {
            return false;
        }
        return !golden.expectOrphanHint()
                || plan.promptContext().contains("可能孤儿")
                || plan.qualityNotes().stream().anyMatch(note -> note.contains("孤儿"));
    }

    private static List<GoldenQuestion> loadGoldenQuestions() throws IOException {
        Path file = resolveGoldenQuestionFile();
        List<GoldenQuestion> questions = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            assertThat(columns).as("golden question row: %s", line).hasSize(7);
            questions.add(new GoldenQuestion(
                    columns[0],
                    columns[1],
                    columns[2],
                    columns[3],
                    List.of(columns[4].split("\\|")),
                    Boolean.parseBoolean(columns[5])));
        }
        assertThat(questions).isNotEmpty();
        return List.copyOf(questions);
    }

    private static Path resolveGoldenQuestionFile() {
        Path fromRoot = Path.of("worklog/v1.0.0/sprint-26-202605/it/sql/flowerbiz_traversal_golden_questions.tsv");
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Path.of("../worklog/v1.0.0/sprint-26-202605/it/sql/flowerbiz_traversal_golden_questions.tsv");
    }

    private record GoldenQuestion(
            String id,
            String question,
            String domain,
            String expectedPath,
            List<String> expectedSourceRefs,
            boolean expectOrphanHint) {
    }
}
