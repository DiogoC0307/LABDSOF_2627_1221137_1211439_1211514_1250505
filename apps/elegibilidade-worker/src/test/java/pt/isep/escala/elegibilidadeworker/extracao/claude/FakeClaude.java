package pt.isep.escala.elegibilidadeworker.extracao.claude;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeSet;

/**
 * A stand-in for the {@code claude} CLI in tests (no network, no login, no dependency but the JDK). Started as
 * {@code java -cp <test-classes> FakeClaude <mode> <record-file> <the extractor's arguments...>}.
 *
 * <p>It records what it was given (arguments, standard input, environment, working directory, its pid) in the record
 * file, then answers as {@code claude -p --output-format json} would, in the way {@code mode} says.
 */
public final class FakeClaude {

	static final String OK = "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,\"result\":\"\","
			+ "\"structured_output\":{\"periods\":[{\"startDate\":\"2026-10-31\",\"endDate\":\"2026-11-01\"},"
			+ "{\"startDate\":\"2026-10-26\",\"endDate\":\"2026-11-30\",\"weekdays\":[\"SUN\"],\"startTime\":\"08:00\","
			+ "\"endTime\":\"14:00\"}]},\"total_cost_usd\":0.00123,"
			+ "\"modelUsage\":{\"claude-haiku-4-5-20251001\":{\"inputTokens\":812,\"outputTokens\":40}}}";

	private FakeClaude() {
	}

	public static void main(String[] args) throws Exception {
		String mode = args[0];
		Path record = Path.of(args[1]);
		byte[] stdin = System.in.readAllBytes();
		Properties seen = new Properties();
		seen.setProperty("pid", String.valueOf(ProcessHandle.current().pid()));
		Path cwd = Path.of("").toAbsolutePath();
		seen.setProperty("cwd", cwd.toString());
		File[] files = cwd.toFile().listFiles();
		seen.setProperty("cwdFiles", String.valueOf(files == null ? -1 : files.length));
		seen.setProperty("args", String.valueOf(args.length - 2));
		for (int i = 2; i < args.length; i++) {
			seen.setProperty("arg." + (i - 2), args[i]);
		}
		seen.setProperty("stdin", new String(stdin, StandardCharsets.UTF_8));
		seen.setProperty("env", String.join(",", new TreeSet<>(System.getenv().keySet())));
		String token = System.getenv("CLAUDE_CODE_OAUTH_TOKEN");
		seen.setProperty("token", token == null ? "" : token);
		try (OutputStream out = Files.newOutputStream(record)) {
			seen.store(out, "FakeClaude");
		}
		answer(mode, new String(stdin, StandardCharsets.UTF_8), token);
	}

	private static void answer(String mode, String stdin, String token) throws IOException, InterruptedException {
		String periodsOk = "{\"type\":\"result\",\"is_error\":false,\"result\":\"\",\"structured_output\":%s,"
				+ "\"total_cost_usd\":0.0009}";
		if (mode.equals("by-text")) {
			// one fake for several cases in one Spring context: the message says which answer to give
			mode = stdin.contains("DORME") ? "sleep" : stdin.contains("LONGE") ? "far" : stdin.contains("ERRO")
					? "error-exit1" : "ok";
		}
		switch (mode) {
			case "ok" -> print(OK);
			case "ok-empty" -> print(periodsOk.formatted("{\"periods\":[]}"));
			case "error-exit0" -> print(error());
			case "error-exit1" -> {
				print(error());
				System.exit(1);
			}
			case "not-json" -> print("Desculpe, não consigo ajudar com isso.");
			case "no-structured" -> print("{\"type\":\"result\",\"is_error\":false,\"result\":\"sexta\"}");
			case "start-without-end" -> print(periodsOk.formatted(
					"{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":\"2026-10-30\",\"startTime\":\"08:00\"}]}"));
			case "extra-field" -> print(periodsOk.formatted("{\"periods\":[],\"note\":\"tem consulta no hospital\"}"));
			case "period-extra-field" -> print(periodsOk.formatted(
					"{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":\"2026-10-30\",\"reason\":\"consulta\"}]}"));
			case "weekday-full" -> print(periodsOk.formatted(
					"{\"periods\":[{\"startDate\":\"2026-10-26\",\"endDate\":\"2026-11-30\",\"weekdays\":[\"SUNDAY\"]}]}"));
			case "past" -> print(periodsOk.formatted("{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":"
					+ "\"2026-10-30\"},{\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-01\"}]}"));
			case "far" -> print(periodsOk.formatted("{\"periods\":[{\"startDate\":\"2026-10-30\",\"endDate\":"
					+ "\"2026-10-30\"},{\"startDate\":\"2027-05-03\",\"endDate\":\"2027-05-03\"}]}"));
			case "sleep" -> {
				Thread.sleep(60_000);
				print(OK);
			}
			case "echo-stderr" -> {
				// a misbehaving CLI that echoes the message and the token: the extractor must never read this
				System.err.println("stdin=" + stdin + " token=" + token);
				print(OK);
			}
			default -> {
				System.err.println("unknown mode");
				System.exit(3);
			}
		}
	}

	private static String error() {
		return "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":true,"
				+ "\"result\":\"Not logged in · Please run /login\",\"total_cost_usd\":0}";
	}

	private static void print(String json) {
		System.out.write(json.getBytes(StandardCharsets.UTF_8), 0, json.getBytes(StandardCharsets.UTF_8).length);
		System.out.flush();
	}
}
