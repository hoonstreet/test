package kr.co.isahaedream.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeploymentService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Seoul"));

    private final Path directory;
    private final boolean enabled;

    public DeploymentService(@Value("${app.deployment.directory:}") String directory) {
        this.enabled = directory != null && !directory.isBlank();
        this.directory = enabled ? Path.of(directory).toAbsolutePath().normalize() : null;
    }

    public DeploymentStatus status() {
        if (!enabled) return new DeploymentStatus(false, "disabled", "사용 안 함", null, null, null);
        Path statusFile = directory.resolve("deploy.status");
        if (!Files.isRegularFile(statusFile)) {
            return new DeploymentStatus(true, "idle", "배포 대기", null, null, null);
        }
        try {
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(statusFile)) {
                int separator = line.indexOf('=');
                if (separator > 0) values.put(line.substring(0, separator), line.substring(separator + 1));
            }
            String state = values.getOrDefault("state", "idle");
            return new DeploymentStatus(true, state, stateLabel(state),
                    blankToNull(values.get("commit")), blankToNull(values.get("message")),
                    displayTime(values.get("updatedAt")));
        } catch (IOException e) {
            return new DeploymentStatus(true, "unknown", "상태 확인 실패", null, e.getMessage(), null);
        }
    }

    public void request(String requestedBy) throws IOException {
        if (!enabled) throw new IllegalStateException("운영 배포 기능이 서버에 설정되지 않았습니다.");
        DeploymentStatus current = status();
        if (current.running()) throw new IllegalStateException("이미 배포가 진행 중입니다.");
        Files.createDirectories(directory);
        Path request = directory.resolve("deploy.request");
        if (Files.exists(request)) throw new IllegalStateException("처리 대기 중인 배포 요청이 있습니다.");
        Path temporary = Files.createTempFile(directory, ".deploy-request-", ".tmp");
        try {
            Files.writeString(temporary,
                    "requestedAt=" + Instant.now() + System.lineSeparator()
                            + "requestedBy=" + safeValue(requestedBy) + System.lineSeparator(),
                    StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temporary, request, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String safeValue(String value) {
        return value == null ? "admin" : value.replaceAll("[^A-Za-z0-9._@-]", "");
    }

    private static String displayTime(String value) {
        try {
            return value == null || value.isBlank() ? null : DISPLAY_TIME.format(Instant.parse(value));
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private static String stateLabel(String state) {
        return switch (state) {
            case "requested" -> "배포 요청됨";
            case "running" -> "배포 진행 중";
            case "succeeded" -> "최근 배포 성공";
            case "failed" -> "최근 배포 실패";
            default -> "배포 대기";
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record DeploymentStatus(boolean enabled, String state, String label,
                                   String commit, String message, String updatedAt) {
        public boolean running() {
            return "requested".equals(state) || "running".equals(state);
        }
    }
}
