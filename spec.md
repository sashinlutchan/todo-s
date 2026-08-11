# TODO: Telemetry (Loki + Alloy + Grafana)

Replaces the SigNoz approach (fully removed — no foundryctl, no `observability/` dir, no
curl-installed CLIs). Constraints: **Docker and Docker Compose only**, nothing else installed
on the host. No raw GitHub release URLs in the Dockerfile — fetch the OTel Java agent via Maven
(it's published to Maven Central as `io.opentelemetry.javaagent:opentelemetry-javaagent`).

## 1. Backend: get the OTel Java agent via Maven, not a GitHub URL

- `backend/todo/Dockerfile` currently has (needs to change):
  ```
  ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.30.0/opentelemetry-javaagent.jar /app/otel-javaagent.jar
  ```
- Confirmed on Maven Central: `io.opentelemetry.javaagent:opentelemetry-javaagent`, latest
  version `2.30.0` (matches the GitHub release tag — same artifact, just also published to
  Maven Central). Metadata: https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/maven-metadata.xml
- Plan: add a `maven-dependency-plugin` execution to `pom.xml` (`copy` goal, bound to
  `package` phase) that pulls `io.opentelemetry.javaagent:opentelemetry-javaagent:2.30.0:jar`
  into `target/otel/opentelemetry-javaagent.jar` — NOT as a regular `<dependency>` (it's a
  shaded agent jar, shouldn't be on the app's compile/runtime classpath).
- Dockerfile build stage already runs `./mvnw clean package`, so this comes along for free.
  Final stage: `COPY --from=build /app/target/otel/opentelemetry-javaagent.jar /app/otel-javaagent.jar`
  instead of the `ADD` line. Entrypoint stays `-javaagent:/app/otel-javaagent.jar`.

## 2. Observability stack: Loki + Alloy + Grafana, plain docker-compose only

Recreate `observability/` with a **plain `docker-compose.yml`** (no installer scripts, no
generated files) running three official images:
- `grafana/loki` — log storage
- `grafana/alloy` — OTel collector: receives OTLP (gRPC 4317 / HTTP 4318) from the backend's
  Java agent, converts logs to Loki push format, forwards to Loki
- `grafana/grafana` — UI, with Loki pre-provisioned as a datasource

Needs config files (to write carefully, not from memory — verify exact syntax against current
Grafana Alloy docs before writing, since Alloy's config language has changed between versions):
- `observability/loki-config.yaml` — minimal single-binary Loki config, filesystem storage.
- `observability/alloy-config.alloy` — Alloy config in its native syntax defining an
  `otelcol.receiver.otlp` component (grpc+http listeners) piped through to a Loki exporter path
  (`otelcol.exporter.loki` → `loki.write`, pointed at the Loki service's push endpoint). Was
  about to verify the exact component/argument names against
  https://grafana.com/docs/alloy/latest/reference/components/otelcol/otelcol.receiver.otlp/ and
  .../otelcol.exporter.loki/ when this got interrupted — do that before writing the file.
- `observability/grafana-datasources.yaml` — provisioning file so Grafana starts with Loki
  already added as a datasource (no manual click-through).

Since only Loki (logs) is in scope — no Tempo (traces) or Mimir/Prometheus (metrics) — the
backend's OTel agent should be told to only export logs and skip traces/metrics, to avoid noisy
failed-export errors with nowhere to go:
```
OTEL_LOGS_EXPORTER=otlp
OTEL_TRACES_EXPORTER=none
OTEL_METRICS_EXPORTER=none
OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317   # -> Alloy, not Loki directly
```

## 3. Wire the backend compose file to it

`backend/todo/docker-compose.yml` already has the OTel env block from the SigNoz attempt aimed
at `host.docker.internal:4317` — reusable as-is, just update the exporter env vars per above
(drop traces/metrics exporters) and the comment referencing SigNoz/`observability/README.md`.

## Status

- [x] SigNoz fully removed (`observability/` deleted, `.gitignore` entry reverted)
- [ ] `pom.xml` — add maven-dependency-plugin execution for the OTel agent jar
- [ ] `Dockerfile` — swap the `ADD` GitHub URL for the Maven-fetched copy
- [ ] `observability/docker-compose.yml` — Loki + Alloy + Grafana
- [ ] `observability/loki-config.yaml`
- [ ] `observability/alloy-config.alloy` (verify syntax against current Alloy docs first)
- [ ] `observability/grafana-datasources.yaml`
- [ ] `backend/todo/docker-compose.yml` — update OTel env vars (logs-only)
- [ ] `backend/todo/.env.example` — update comment (was pointing at SigNoz)
- [ ] Rebuild backend, confirm it still starts clean with the Maven-fetched agent attached
- [ ] Bring up the Loki/Alloy/Grafana stack, confirm logs actually show up in Grafana
