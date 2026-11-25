package com.mini_banking_app.architecture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Архітектурні тести (fitness functions) модульного моноліту.
 * Аналізуються імпорти у вихідному коді, тож тест не потребує сторонніх бібліотек.
 * Порушення будь-якого правила «ламає» збірку — архітектура не деградує непомітно.
 */
class ModuleArchitectureTest {

    private static final String BASE = "com.mini_banking_app.";
    private static final Path SOURCES = Path.of("src/main/java/com/mini_banking_app");
    private static final Set<String> MODULES = Set.of("user", "account", "ledger", "notification");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)\\s*;", Pattern.MULTILINE);

    private record SourceFile(Path path, String module, String layer, List<String> imports) {
    }

    private List<SourceFile> files;

    @BeforeEach
    void scan() {
        files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(SOURCES)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".java")).toList()) {
                String[] parts = SOURCES.relativize(p).toString().replace('\\', '/').split("/");
                String module = parts.length > 1 ? parts[0] : "(root)";
                String layer = parts.length > 2 ? parts[1] : "";
                List<String> imports = new ArrayList<>();
                Matcher m = IMPORT.matcher(Files.readString(p));
                while (m.find()) {
                    imports.add(m.group(1));
                }
                files.add(new SourceFile(p, module, layer, imports));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(files.size() > 20, "Не знайдено вихідних файлів: " + SOURCES.toAbsolutePath());
    }

    private static String targetModule(String imp) {
        return imp.startsWith(BASE) ? imp.substring(BASE.length()).split("\\.")[0] : null;
    }

    private static String targetLayer(String imp) {
        String[] parts = imp.substring(BASE.length()).split("\\.");
        return parts.length > 1 ? parts[1] : "";
    }

    @Test
    @DisplayName("Модулі звертаються один до одного лише через пакет api")
    void crossModuleAccessOnlyThroughApi() {
        List<String> violations = new ArrayList<>();
        for (SourceFile f : files) {
            for (String imp : f.imports()) {
                String target = targetModule(imp);
                if (target != null && MODULES.contains(target) && !target.equals(f.module())
                        && !"api".equals(targetLayer(imp))) {
                    violations.add(f.path() + " -> " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Доступ в обхід api: " + violations);
    }

    @Test
    @DisplayName("Жоден модуль не залежить від шару web, а shared — від модулів")
    void layeringOfWebAndShared() {
        List<String> violations = new ArrayList<>();
        for (SourceFile f : files) {
            for (String imp : f.imports()) {
                String target = targetModule(imp);
                if ("web".equals(target) && !"web".equals(f.module())) {
                    violations.add(f.path() + " -> " + imp);
                }
                if ("shared".equals(f.module()) && target != null && !"shared".equals(target)) {
                    violations.add(f.path() + " -> " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Порушення шарів: " + violations);
    }

    @Test
    @DisplayName("Доменний шар не залежить від Spring і Servlet API")
    void domainIsFrameworkFree() {
        List<String> violations = new ArrayList<>();
        for (SourceFile f : files) {
            boolean domain = "domain".equals(f.layer()) || ("shared".equals(f.module()) && "domain".equals(f.layer()));
            if (!domain) {
                continue;
            }
            for (String imp : f.imports()) {
                if (imp.startsWith("org.springframework") || imp.startsWith("jakarta.servlet")) {
                    violations.add(f.path() + " -> " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Залежність домену від фреймворку: " + violations);
    }

    @Test
    @DisplayName("Контракт api не залежить від внутрішніх шарів свого модуля")
    void apiIsSelfContained() {
        List<String> violations = new ArrayList<>();
        for (SourceFile f : files) {
            if (!"api".equals(f.layer())) {
                continue;
            }
            for (String imp : f.imports()) {
                if (f.module().equals(targetModule(imp)) && !"api".equals(targetLayer(imp))) {
                    violations.add(f.path() + " -> " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(), "api посилається на реалізацію: " + violations);
    }

    @Test
    @DisplayName("Шар web не працює з репозиторіями та JPA напряму")
    void webHasNoPersistence() {
        List<String> violations = new ArrayList<>();
        for (SourceFile f : files) {
            if (!"web".equals(f.module())) {
                continue;
            }
            for (String imp : f.imports()) {
                if (imp.startsWith("org.springframework.data") || imp.startsWith("jakarta.persistence")) {
                    violations.add(f.path() + " -> " + imp);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Контролер звертається до шару даних: " + violations);
    }

    @Test
    @DisplayName("Граф залежностей між модулями не містить циклів")
    void noCycles() {
        Map<String, Set<String>> graph = new HashMap<>();
        for (SourceFile f : files) {
            if (!MODULES.contains(f.module())) {
                continue;
            }
            for (String imp : f.imports()) {
                String target = targetModule(imp);
                if (target != null && MODULES.contains(target) && !target.equals(f.module())) {
                    graph.computeIfAbsent(f.module(), k -> new HashSet<>()).add(target);
                }
            }
        }
        for (String start : MODULES) {
            assertTrue(!reachable(graph, start, start, new HashSet<>()), "Циклічна залежність через модуль " + start + ": " + graph);
        }
    }

    private static boolean reachable(Map<String, Set<String>> graph, String from, String target, Set<String> seen) {
        for (String next : graph.getOrDefault(from, Set.of())) {
            if (next.equals(target)) {
                return true;
            }
            if (seen.add(next) && reachable(graph, next, target, seen)) {
                return true;
            }
        }
        return false;
    }
}
