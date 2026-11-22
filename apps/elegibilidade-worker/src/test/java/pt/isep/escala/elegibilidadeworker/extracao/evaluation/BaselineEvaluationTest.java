package pt.isep.escala.elegibilidadeworker.extracao.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.isep.escala.elegibilidadeworker.extracao.baseline.RuleBasedExtractor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TC-SCRUM-99-09. One run of the rule-based baseline on the 60 cases of the evaluation dataset, reported to
 * {@code target/ai-eval/baseline-results.json} and {@code .md}.
 *
 * <p>The run reports; it never fails the build because a measure is below the targets of 06 · Responsible AI
 * Opportunity Assessment (they are in the report, with a met / not met column). It only fails if the dataset cannot be
 * read or is not the dataset the README describes.
 *
 * <p>The dataset was written on 2026-10-25, before the parser, and is read only here. The dataset commit is passed as
 * {@code -Dai-eval.dataset-commit=<hash>} and the parser label as {@code -Dai-eval.parser-version=<label>} (default
 * "v0"); the SHA-256 of the file is always recorded, so the report says which file it was run on.
 */
class BaselineEvaluationTest {

	private static final String DATASET = "/ai-eval/unavailability.jsonl";
	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	@Test
	@DisplayName("TC-SCRUM-99-09 the baseline runs once on the 60 cases and the results are reported (the targets are reported, not enforced)")
	void runsOnTheDatasetAndReports() throws IOException, NoSuchAlgorithmException {
		byte[] bytes;
		try (InputStream in = getClass().getResourceAsStream(DATASET)) {
			assertThat(in).as("the dataset " + DATASET).isNotNull();
			bytes = in.readAllBytes();
		}
		List<EvaluationCase> cases = EvaluationCase.readAll(new ByteArrayInputStream(bytes), MAPPER);

		EvaluationReport report = new EvaluationScorer(new RuleBasedExtractor(),
				System.getProperty("ai-eval.parser-version", "v0")).score(cases)
				.withDataset(System.getProperty("ai-eval.dataset-commit"),
						HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));

		Path out = Path.of("target", "ai-eval");
		Files.createDirectories(out);
		Files.writeString(out.resolve("baseline-results.json"), report.toJson(MAPPER), StandardCharsets.UTF_8);
		Files.writeString(out.resolve("baseline-results.md"), report.toMarkdown(), StandardCharsets.UTF_8);

		// the dataset is the one the README describes: 60 cases, 35 normal / 15 difficult / 10 adverse
		assertThat(report.overall().cases()).isEqualTo(60);
		assertThat(report.category("normal").cases()).isEqualTo(35);
		assertThat(report.category("difficult").cases()).isEqualTo(15);
		assertThat(report.category("adverse").cases()).isEqualTo(10);
		assertThat(out.resolve("baseline-results.json")).exists();
		assertThat(out.resolve("baseline-results.md")).exists();
	}
}
