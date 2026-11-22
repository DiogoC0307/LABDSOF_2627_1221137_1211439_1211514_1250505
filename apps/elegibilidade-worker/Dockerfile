# Build context is this folder:  docker build -t ghcr.io/<user>/<service>:latest apps/<service>
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

# OpenTelemetry Java agent (distributed tracing). It is downloaded and VERIFIED here, so a bad download
# fails the build instead of producing an image that cannot start. It is only active when
# JAVA_TOOL_OPTIONS points at it (set in the Kubernetes ConfigMaps and in compose.yaml).
FROM eclipse-temurin:21-jdk AS otel
ARG OTEL_AGENT_VERSION=2.26.1
RUN mkdir -p /otel \
 && ( curl -fsSL -o /otel/opentelemetry-javaagent.jar "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
      || wget -q -O /otel/opentelemetry-javaagent.jar "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" ) \
 && jar tf /otel/opentelemetry-javaagent.jar | grep -q '^META-INF/MANIFEST.MF$' \
 && chmod 755 /otel && chmod 644 /otel/opentelemetry-javaagent.jar

# Claude Code CLI (SCRUM-90, ADR 0016). npm installs it here, at a FIXED version, and the build fails if it does
# not start, like the OpenTelemetry agent above. Node is used ONLY in this stage, to run npm: the npm package is a
# thin wrapper and the CLI itself is a native binary (the platform package, e.g. @anthropic-ai/claude-code-linux-x64),
# so the final image gets that one file and no Node. `cp -L` follows the link npm leaves as bin/claude.exe, which
# also makes this independent of the CPU architecture. The CLI runs only when the worker calls it (AI_ENABLED,
# ADR 0011); nothing starts it at boot.
# To upgrade: change CLAUDE_CODE_VERSION (npm dist-tag "stable") and NODE_VERSION, rebuild, and run `claude --version`.
FROM node:24.22.3-slim AS claude
ARG CLAUDE_CODE_VERSION=2.1.323
RUN npm install -g "@anthropic-ai/claude-code@${CLAUDE_CODE_VERSION}" \
 && mkdir /out \
 && cp -L /usr/local/lib/node_modules/@anthropic-ai/claude-code/bin/claude.exe /out/claude \
 && /out/claude --version | grep -q "^${CLAUDE_CODE_VERSION} (Claude Code)"

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=otel /otel /otel
RUN chmod -R a+rX /otel
COPY --from=claude /out/claude /usr/local/bin/claude
RUN chmod 755 /usr/local/bin/claude && claude --version
# The CLI keeps its settings in a folder it can write to as user 1000. No updates and no non-essential traffic.
# No credential is set here: the token is CLAUDE_CODE_OAUTH_TOKEN, injected at run time (never ANTHROPIC_API_KEY).
ENV CLAUDE_CONFIG_DIR=/tmp/claude-config \
    DISABLE_AUTOUPDATER=1 \
    CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar
USER 1000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
