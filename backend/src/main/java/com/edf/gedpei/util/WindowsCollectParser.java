package com.edf.gedpei.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lit le rapport texte produit par le script de collecte Windows en lecture seule
 * ({@code collecte-windows-lecture-seule.ps1}) et en extrait les composants
 * reellement installes : Tomcat, JDK, applicatifs IDOX, outils tiers, ainsi que
 * les constats de configuration (certificat HTTPS, FTP, AJP, applis de gestion).
 *
 * <p>Prevobs/Inventiv ne voit sur Windows que l'OS et l'agent FlexNet, et lit
 * la version de Tomcat dans le registre (celle de l'installeur d'origine) : ce
 * rapport est la seule source fiable pour la couche applicative.</p>
 */
public final class WindowsCollectParser {

    /** Repertoires consideres comme des copies inactives (sauvegardes, livraisons). */
    private static final Pattern COPY_PATH = Pattern.compile(
            "(?i)\\\\(backup[^\\\\]*|save[^\\\\]*|sav|livraison|livrable|old[^\\\\]*|[^\\\\]*_old[^\\\\]*|[^\\\\]*_bak[^\\\\]*"
                    + "|[^\\\\]*backup[^\\\\]*|software[s]?|staging|pack|oldtomcat|[^\\\\]*_new|new version|partage)\\\\");

    private static final Pattern SECTION = Pattern.compile("^== (\\d+)\\. ");
    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private WindowsCollectParser() {
    }

    /** Composant ou constat extrait du rapport. */
    public record Component(String softwareName, String version, String role,
                            List<String> paths, String details, String forcedStatus,
                            String supportEndDate) {
    }

    /** Resultat de la lecture d'un rapport. */
    public record Result(String hostname, String os, LocalDate collectedAt,
                         List<Component> components) {
    }

    public static Result parse(String text) {
        Map<Integer, List<String>> sections = splitSections(text);
        List<String> s1 = sections.getOrDefault(1, List.of());

        String hostname = valueAfter(s1, "Serveur");
        String os = valueAfter(s1, "OS");
        LocalDate collectedAt = parseDate(firstToken(valueAfter(s1, "Date collecte")));
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Rapport de collecte Windows non reconnu (ligne 'Serveur :' absente)");
        }

        List<Component> out = new ArrayList<>();
        parseOs(os, s1, out);

        Map<String, String> runningServices = parseServices(sections.getOrDefault(3, List.of()));
        Map<String, String[]> procrun = parseProcrun(sections.getOrDefault(4, List.of()));
        Set<String> runningExecutables = parseProcessPaths(sections.getOrDefault(5, List.of()));
        Set<Integer> listeningPorts = parsePorts(sections.getOrDefault(6, List.of()));

        // Tomcat et JVM effectivement utilises : services Tomcat en cours d'execution.
        Set<String> activeTomcatDirs = new HashSet<>();
        Set<String> activeJvmDirs = new HashSet<>();
        Set<String> stoppedTomcatDirs = new HashSet<>();
        Set<String> stoppedJvmDirs = new HashSet<>();
        procrun.forEach((service, cfg) -> {
            boolean running = "Running".equalsIgnoreCase(runningServices.get(service));
            if (cfg[1] != null) (running ? activeTomcatDirs : stoppedTomcatDirs).add(norm(cfg[1]));
            if (cfg[0] != null) (running ? activeJvmDirs : stoppedJvmDirs).add(norm(cfg[0]));
        });
        stoppedTomcatDirs.removeAll(activeTomcatDirs);
        stoppedJvmDirs.removeAll(activeJvmDirs);

        parseJavas(sections.getOrDefault(7, List.of()), activeJvmDirs, stoppedJvmDirs, runningExecutables, out);
        parseTomcats(sections.getOrDefault(8, List.of()), activeTomcatDirs, stoppedTomcatDirs, listeningPorts, out);
        parseRegistry(sections.getOrDefault(2, List.of()), out);
        parseApplications(text, sections, out);
        parseOpenSsl(sections.getOrDefault(11, List.of()), out);
        parseFtp(sections.getOrDefault(15, List.of()), out);
        parseHttpsCertificate(sections.getOrDefault(16, List.of()), out);

        return new Result(hostname.trim(), os, collectedAt, out);
    }

    // ------------------------------------------------------------------ sections

    private static Map<Integer, List<String>> splitSections(String text) {
        Map<Integer, List<String>> sections = new HashMap<>();
        int current = 0;
        for (String raw : text.replace("\uFEFF", "").split("\\r?\\n")) {
            Matcher m = SECTION.matcher(raw);
            if (m.find()) {
                current = Integer.parseInt(m.group(1));
                continue;
            }
            if (raw.startsWith("=====")) continue;
            sections.computeIfAbsent(current, k -> new ArrayList<>()).add(raw);
        }
        return sections;
    }

    private static void parseOs(String os, List<String> s1, List<Component> out) {
        if (os == null) return;
        Matcher m = Pattern.compile("(?i)Windows Server (\\d{4}(?: R2)?) (\\w+)").matcher(os);
        if (!m.find()) return;
        String version = "SERVER " + m.group(1).toUpperCase() + " " + m.group(2).toUpperCase();

        // Date du dernier correctif installe (colonne InstalledOn, jj/mm/aaaa).
        LocalDate lastPatch = null;
        String lastKb = null;
        Pattern kb = Pattern.compile("^(KB\\d+)\\s+.*?(\\d{2}/\\d{2}/\\d{4})");
        for (String l : s1) {
            Matcher k = kb.matcher(l.trim());
            if (k.find()) {
                LocalDate d = parseDate(k.group(2));
                if (d != null && (lastPatch == null || d.isAfter(lastPatch))) {
                    lastPatch = d;
                    lastKb = k.group(1);
                }
            }
        }
        String forced = m.group(1).startsWith("2012") || m.group(1).startsWith("2008") ? "OBSOLETE" : null;
        out.add(new Component("WINDOWS SERVER", version, "OS", List.of(),
                lastPatch != null ? "Dernier correctif : " + lastKb + " du " + lastPatch.format(FR_DATE) : null,
                forced, null));

        if (lastPatch != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(lastPatch, LocalDate.now());
            String status = days > 365 ? "INTERDITE_CYBER" : days > 90 ? "RISQUEE" : "PRECONISEE";
            out.add(new Component("CORRECTIFS WINDOWS", lastKb + " (" + lastPatch.format(FR_DATE) + ")",
                    "constat", List.of(), "Dernier correctif installe il y a " + days + " jours", status, null));
        }
    }

    /** Nom du service -> etat (Running/Stopped). */
    private static Map<String, String> parseServices(List<String> lines) {
        Map<String, String> states = new HashMap<>();
        String name = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("Name ") || t.startsWith("Name:")) name = after(t);
            else if ((t.startsWith("State ") || t.startsWith("State:")) && name != null) states.put(name, after(t));
        }
        return states;
    }

    /** Service Tomcat -> {repertoire JVM, repertoire Tomcat}. */
    private static Map<String, String[]> parseProcrun(List<String> lines) {
        Map<String, String[]> cfg = new LinkedHashMap<>();
        String service = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("--- Service :")) {
                service = t.substring("--- Service :".length()).trim();
                cfg.put(service, new String[2]);
            } else if (service != null && t.startsWith("Jvm ")) {
                String jvm = after(t).replace("\\\\", "\\");
                int i = jvm.toLowerCase().indexOf("\\bin\\");
                cfg.get(service)[0] = i > 0 ? jvm.substring(0, i) : jvm;
            } else if (service != null && t.startsWith("Classpath ")) {
                String first = after(t).split(";")[0];
                int i = first.toLowerCase().indexOf("\\bin\\");
                cfg.get(service)[1] = i > 0 ? first.substring(0, i) : first;
            }
        }
        return cfg;
    }

    private static Set<String> parseProcessPaths(List<String> lines) {
        Set<String> paths = new HashSet<>();
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("ExecutablePath")) paths.add(norm(after(t)));
        }
        return paths;
    }

    private static Set<Integer> parsePorts(List<String> lines) {
        Set<Integer> ports = new HashSet<>();
        for (String l : lines) {
            String[] p = l.trim().split("\\s+");
            if (p.length >= 3 && p[1].matches("\\d+")) ports.add(Integer.parseInt(p[1]));
        }
        return ports;
    }

    // ------------------------------------------------------------------ Java

    private static void parseJavas(List<String> lines, Set<String> activeJvmDirs, Set<String> stoppedJvmDirs,
                                   Set<String> runningExecutables, List<Component> out) {
        String javaHome = null;
        List<String[]> found = new ArrayList<>(); // {exe, fileVersion, vendor, javaVersion}
        String[] cur = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("--- ") && t.toLowerCase().endsWith("java.exe")) {
                cur = new String[]{t.substring(4).trim(), null, null, null};
                found.add(cur);
            } else if (cur != null && t.startsWith("FileVersion")) {
                String[] v = after(t).split("/", 2);
                cur[1] = v[0].trim();
                cur[2] = v.length > 1 ? v[1].trim() : null;
            } else if (cur != null && t.startsWith("JAVA_VERSION=")) {
                cur[3] = t.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
            } else if (t.startsWith("JAVA_HOME")) {
                javaHome = t.contains("=") ? norm(t.substring(t.indexOf('=') + 1)) : null;
            }
        }

        // Regroupement par (nom, version, role) pour ne pas noyer l'ecran sous les copies.
        Map<String, List<String>> groups = new LinkedHashMap<>();
        Map<String, String[]> meta = new HashMap<>();
        for (String[] j : found) {
            String exe = j[0];
            String dir = exe.replaceAll("(?i)\\\\bin\\\\java\\.exe$", "");
            String vendor = j[2] != null ? j[2] : "";
            String[] version = javaVersion(j[3], j[1]);
            String name = vendor.matches("(?i).*(oracle|sun).*") ? "JAVA ORACLE" : "JAVA OPENJDK";

            String role;
            if (activeJvmDirs.contains(norm(dir))) role = "actif (Tomcat)";
            else if (runningExecutables.contains(norm(exe))) role = "actif (processus)";
            else if (stoppedJvmDirs.contains(norm(dir))) role = "service arrete";
            else if (COPY_PATH.matcher(dir + "\\").find()) role = "copie inactive";
            else if (dir.toLowerCase().matches(".*\\\\(opidis|diw)\\\\.*")) role = "embarque DIW";
            else if (norm(dir).equals(javaHome)) role = "JAVA_HOME";
            else role = "installe";

            String key = name + "|" + version[0] + "|" + role;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(dir);
            meta.put(key, new String[]{name, version[0], version[1], role, vendor});
        }
        groups.forEach((key, dirs) -> {
            String[] m = meta.get(key);
            int major = majorOf(m[1]);
            String forced = major > 0 && major <= 8 ? "OBSOLETE" : null;
            String details = (m[4].isBlank() ? "" : "Editeur : " + m[4] + ". ")
                    + (m[2] != null ? "Version : " + m[2] + ". " : "")
                    + dirs.size() + " occurrence(s)";
            out.add(new Component(m[0], m[1], m[3], dirs, details, forced, null));
        });
    }

    /**
     * Version exploitable par le rapprochement CSR, et libelle lisible.
     * Les JRE 6/7/8 n'ont souvent qu'un FileVersion (8.0.2650.1 = 8u265).
     */
    private static String[] javaVersion(String javaVersion, String fileVersion) {
        String fv = fileVersion != null ? fileVersion : "";
        Matcher legacy = Pattern.compile("^([678])\\.0\\.(\\d+)\\.\\d+").matcher(fv);
        if ((javaVersion == null || javaVersion.startsWith("1.")) && legacy.find()) {
            int update = Integer.parseInt(legacy.group(2)) / 10;
            return new String[]{legacy.group(1) + ".0." + update, legacy.group(1) + "u" + update};
        }
        if (javaVersion != null && !javaVersion.isBlank()) return new String[]{javaVersion, null};
        return new String[]{fv.isBlank() ? "?" : fv, null};
    }

    // ------------------------------------------------------------------ Tomcat

    private static void parseTomcats(List<String> lines, Set<String> activeTomcatDirs, Set<String> stoppedTomcatDirs,
                                     Set<Integer> listeningPorts, List<Component> out) {
        Map<String, List<String>> copies = new LinkedHashMap<>();
        String dir = null;
        String version = null;
        List<String> block = new ArrayList<>();

        List<Object[]> tomcats = new ArrayList<>(); // {dir, version, lines}
        for (String l : lines) {
            if (l.startsWith("--- ")) {
                if (dir != null) tomcats.add(new Object[]{dir, version, block});
                dir = l.substring(4).trim();
                version = null;
                block = new ArrayList<>();
            } else if (l.startsWith("server.info=")) {
                version = l.replaceAll(".*Apache Tomcat/", "").trim();
            } else if (dir != null) {
                block.add(l);
            }
        }
        if (dir != null) tomcats.add(new Object[]{dir, version, block});

        for (Object[] t : tomcats) {
            String d = (String) t[0];
            String v = t[1] != null ? (String) t[1] : "?";
            @SuppressWarnings("unchecked") List<String> b = (List<String>) t[2];
            boolean active = activeTomcatDirs.contains(norm(d));
            int major = majorOf(v);
            String forced = major > 0 && major <= 8 ? "OBSOLETE" : null;

            if (!active && stoppedTomcatDirs.contains(norm(d))) {
                out.add(new Component("TOMCAT", v, "service arrete", List.of(d),
                        "Service Tomcat installe mais arrete : a desinstaller s'il ne sert plus", forced, null));
                continue;
            }
            if (!active) {
                copies.computeIfAbsent(v, k -> new ArrayList<>()).add(d);
                continue;
            }
            String webapps = b.stream().filter(x -> x.trim().startsWith("webapps :")).findFirst()
                    .map(x -> x.trim().substring("webapps :".length()).trim()).orElse("");
            out.add(new Component("TOMCAT", v, "actif", List.of(d), "Webapps : " + webapps, forced, null));

            // Applis de gestion et d'exemple deployees sur le Tomcat actif.
            List<String> admin = new ArrayList<>();
            for (String app : List.of("manager", "host-manager", "examples")) {
                if (Arrays.stream(webapps.split(",\\s*")).anyMatch(app::equalsIgnoreCase)) admin.add(app);
            }
            if (!admin.isEmpty()) {
                out.add(new Component("TOMCAT APPLIS DE GESTION", String.join(", ", admin), "constat",
                        List.of(d), "Webapps d'administration ou d'exemple deployees sur le Tomcat actif : a retirer",
                        "RISQUEE", null));
            }

            // Connecteur AJP actif sans secret.
            boolean ajpNoSecret = b.stream().anyMatch(x -> {
                String line = x.replaceAll("<!--.*?-->", "");
                return line.contains("AJP/1.3") && line.contains("secretRequired=\"false\"")
                        && !line.trim().startsWith("<!--");
            });
            if (ajpNoSecret && listeningPorts.contains(8009)) {
                out.add(new Component("TOMCAT CONNECTEUR AJP", "8009 secretRequired=false", "constat",
                        List.of(d), "Connecteur AJP ouvert sans secret (Ghostcat CVE-2020-1938 si expose)",
                        "RISQUEE", null));
            }
            // Mot de passe de keystore en clair et trivial.
            for (String x : b) {
                Matcher pw = Pattern.compile("(?i)(keystorePass|certificateKeystorePassword)=\"([^\"]*)\"").matcher(x);
                if (pw.find() && !x.trim().startsWith("<!--") && pw.group(2).length() < 10) {
                    out.add(new Component("TOMCAT KEYSTORE", "mot de passe faible en clair", "constat",
                            List.of(d), "Attribut " + pw.group(1) + " en clair dans server.xml (" + pw.group(2).length()
                            + " caracteres)", "RISQUEE", null));
                    break;
                }
            }
        }
        copies.forEach((v, dirs) -> {
            int major = majorOf(v);
            out.add(new Component("TOMCAT", v, "copie inactive", dirs,
                    dirs.size() + " copie(s) hors service a supprimer apres validation",
                    major > 0 && major <= 8 ? "OBSOLETE" : null, null));
        });
    }

    // ------------------------------------------------------------------ registre

    private static final Map<String, String> REGISTRY_PRODUCTS = new LinkedHashMap<>();
    static {
        REGISTRY_PRODUCTS.put("(?i)^Brava! Enterprise", "OPENTEXT BRAVA");
        REGISTRY_PRODUCTS.put("(?i)^Google Chrome", "GOOGLE CHROME");
        REGISTRY_PRODUCTS.put("(?i)^Strawberry Perl", "STRAWBERRY PERL");
        REGISTRY_PRODUCTS.put("(?i)^svr_cygwin$", "CYGWIN");
        REGISTRY_PRODUCTS.put("(?i)^Notepad\\+\\+", "NOTEPAD++");
        REGISTRY_PRODUCTS.put("(?i)^WinSCP", "WINSCP");
        REGISTRY_PRODUCTS.put("(?i)^FileZilla", "FILEZILLA");
        REGISTRY_PRODUCTS.put("(?i)^PuTTY", "PUTTY");
        REGISTRY_PRODUCTS.put("(?i)^Adobe Acrobat Reader", "ADOBE ACROBAT READER");
        REGISTRY_PRODUCTS.put("(?i)^DB Browser for SQLite", "DB BROWSER FOR SQLITE");
        REGISTRY_PRODUCTS.put("(?i)^Dynatrace OneAgent", "DYNATRACE ONEAGENT");
    }

    private static void parseRegistry(List<String> lines, List<Component> out) {
        int verCol = -1;
        int pubCol = -1;
        Set<String> seen = new HashSet<>();
        for (String l : lines) {
            if (l.startsWith("DisplayName") && l.contains("DisplayVersion")) {
                verCol = l.indexOf("DisplayVersion");
                pubCol = l.indexOf("Publisher");
                continue;
            }
            if (verCol < 0 || l.length() <= verCol || l.startsWith("---")) continue;
            String name = l.substring(0, verCol).trim();
            String version = l.substring(verCol, pubCol > verCol && l.length() > pubCol ? pubCol : l.length()).trim();
            for (Map.Entry<String, String> p : REGISTRY_PRODUCTS.entrySet()) {
                if (name.matches(p.getKey() + ".*") && seen.add(p.getValue())) {
                    out.add(new Component(p.getValue(), version.isBlank() ? "?" : version, "installe",
                            List.of(), "Registre : " + name, null, null));
                }
            }
        }
    }

    // ------------------------------------------------------------------ IDOX / tiers

    private static void parseApplications(String text, Map<Integer, List<String>> sections, List<Component> out) {
        // OLM : version du jar olm-admin (la plus recente presente).
        String olm = maxVersion(text, "olm-admin-(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.jar");
        if (olm != null) {
            out.add(new Component("IDOX OLM", olm, "installe", List.of(), "Version du jar olm-admin", null, null));
        } else if (text.contains("olm-admin.jar")) {
            out.add(new Component("IDOX OLM", "olm-admin.jar (version non numerotee)", "installe", List.of(),
                    "Ancien format de livraison OLM", "OBSOLETE", null));
        }
        // DIW : version du moteur de planification, le seul jar versionne de DIW.
        String diw = maxVersion(text, "workshop-schedule-(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.jar");
        if (diw == null && text.matches("(?s).*\\\\Opidis\\\\diw.*")) diw = "version non lue";
        if (diw != null) {
            out.add(new Component("IDOX DIW", diw, "installe", List.of(), "Version de workshop-schedule", null, null));
        }

        List<String> s10 = sections.getOrDefault(10, List.of());
        Map<String, List<String>> sqlite = new LinkedHashMap<>();
        for (String l : s10) {
            Matcher m = Pattern.compile("^(.*sqlite-jdbc-([\\d.]+)\\.jar)").matcher(l.trim());
            if (m.find()) sqlite.computeIfAbsent(m.group(2), k -> new ArrayList<>()).add(m.group(1));
        }
        sqlite.forEach((v, paths) -> out.add(new Component("SQLITE-JDBC", v, "embarque OLM", paths,
                paths.size() + " occurrence(s)", majorOf(v) <= 3 && v.startsWith("3.7") ? "OBSOLETE" : null, null)));

        Set<String> awstats = new LinkedHashSet<>();
        for (String l : s10) {
            Matcher m = Pattern.compile("\\$VERSION\\s*=\\s*\"([\\d.]+)").matcher(l);
            if (m.find()) awstats.add(m.group(1));
        }
        for (String v : awstats) {
            out.add(new Component("AWSTATS", v, "installe", List.of(), "Script Perl awstats.pl",
                    v.compareTo("7.9") < 0 ? "OBSOLETE" : null, null));
        }
    }

    private static void parseOpenSsl(List<String> lines, List<Component> out) {
        Map<String, List<String>> byVersion = new LinkedHashMap<>();
        for (String l : lines) {
            String[] p = l.trim().split("\\s+\\|\\s*");
            if (p.length < 2 || p[0].toLowerCase().contains("tcnative")) continue;
            String v = p[1].trim();
            if (!v.matches("\\d+\\.\\d+.*")) continue;
            byVersion.computeIfAbsent(v, k -> new ArrayList<>()).add(p[0].trim());
        }
        byVersion.forEach((v, paths) -> out.add(new Component("OPENSSL", v, "embarque (outils)", paths,
                paths.size() + " binaire(s) : Cygwin, Perl, beats, agents...", v.startsWith("1.") ? "OBSOLETE" : null,
                null)));
    }

    // ------------------------------------------------------------------ constats

    private static void parseFtp(List<String> lines, List<Component> out) {
        String site = null;
        boolean ftp = false;
        boolean anonymous = false;
        boolean sslAllow = false;
        boolean openRule = false;
        String path = null;
        List<String[]> sites = new ArrayList<>();
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("Site :")) {
                if (site != null && ftp) sites.add(new String[]{site, path, "" + anonymous, "" + sslAllow, "" + openRule});
                String[] p = t.substring(6).split("\\|");
                site = p[0].trim();
                path = p.length > 1 ? p[1].replace("chemin :", "").trim() : null;
                ftp = false; anonymous = false; sslAllow = false; openRule = false;
            } else if (t.startsWith("ftp ")) {
                ftp = true;
            } else if (t.startsWith("FTP anonyme active")) {
                ftp = true;
                anonymous = t.matches("(?i)FTP anonyme active\\s*:\\s*true.*");
            } else if (t.startsWith("FTP SSL")) {
                sslAllow = t.contains("SslAllow");
            } else if (t.startsWith("Allow users=") && t.contains("Write")) {
                if (t.startsWith("Allow users=? ") || t.startsWith("Allow users=* ")) openRule = true;
            }
        }
        if (site != null && ftp) sites.add(new String[]{site, path, "" + anonymous, "" + sslAllow, "" + openRule});

        for (String[] s : sites) {
            boolean anon = Boolean.parseBoolean(s[2]);
            boolean clear = Boolean.parseBoolean(s[3]);
            boolean open = Boolean.parseBoolean(s[4]);
            if (!anon && !clear && !open) continue;
            String status = anon && open ? "INTERDITE_CYBER" : (clear || open) ? "INTERDITE" : "RISQUEE";
            String details = (anon ? "Authentification anonyme active. " : "")
                    + (open ? "Lecture/ecriture accordee a tous (? ou *). " : "")
                    + (clear ? "SSL facultatif (SslAllow) : mots de passe et fichiers en clair possibles. " : "")
                    + "Racine : " + s[1];
            out.add(new Component("IIS FTP", "site " + s[0], "constat", List.of(s[1] != null ? s[1] : ""),
                    details, status, null));
        }
    }

    private static void parseHttpsCertificate(List<String> lines, List<Component> out) {
        for (String l : lines) {
            String t = l.trim();
            Matcher m = Pattern.compile("^Port (\\d+) : (CN=[^,|]+).*expire le (\\d{2}/\\d{2}/\\d{4})").matcher(t);
            if (!m.find()) continue;
            LocalDate end = parseDate(m.group(3));
            if (end == null) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), end);
            String status = days < 0 ? "INTERDITE_CYBER" : days <= 30 ? "INTERDITE" : days <= 90 ? "RISQUEE" : "PRECONISEE";
            out.add(new Component("CERTIFICAT HTTPS", "port " + m.group(1) + " - expire le " + m.group(3),
                    "constat", List.of(), m.group(2) + " (" + days + " jours restants)", status,
                    end.format(FR_DATE)));
        }
    }

    // ------------------------------------------------------------------ outils

    private static String maxVersion(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        String best = null;
        while (m.find()) {
            if (best == null || compareVersions(m.group(1), best) > 0) best = m.group(1);
        }
        return best;
    }

    static int compareVersions(String a, String b) {
        String[] x = a.split("\\D+");
        String[] y = b.split("\\D+");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int u = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int v = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (u != v) return Integer.compare(u, v);
        }
        return 0;
    }

    private static int majorOf(String version) {
        if (version == null) return -1;
        Matcher m = Pattern.compile("^(\\d+)").matcher(version.trim());
        if (!m.find()) return -1;
        int major = Integer.parseInt(m.group(1));
        // Ancienne numerotation Java 1.x
        if (major == 1) {
            Matcher n = Pattern.compile("^1\\.(\\d+)").matcher(version.trim());
            if (n.find()) return Integer.parseInt(n.group(1));
        }
        return major;
    }

    private static String valueAfter(List<String> lines, String key) {
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith(key) && t.contains(":")) {
                String head = t.substring(0, t.indexOf(':')).trim();
                if (head.equalsIgnoreCase(key)) return t.substring(t.indexOf(':') + 1).trim();
            }
        }
        return null;
    }

    private static String after(String line) {
        int i = line.indexOf(':');
        return i < 0 ? "" : line.substring(i + 1).trim();
    }

    private static String firstToken(String s) {
        return s == null ? null : s.trim().split("\\s+")[0];
    }

    private static LocalDate parseDate(String s) {
        if (s == null) return null;
        try {
            return LocalDate.parse(s.trim(), FR_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String norm(String path) {
        if (path == null) return "";
        String p = path.trim().replace("\"", "").replace("\\\\", "\\").toLowerCase();
        while (p.endsWith("\\")) p = p.substring(0, p.length() - 1);
        return p;
    }
}
