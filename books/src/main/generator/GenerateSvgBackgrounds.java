// =============================================================================
// Generates the abstract SVG backgrounds shared by the books.
//
// Launched by books/pom.xml (exec-maven-plugin, initialize phase) in source-file
// mode (`java GenerateSvgBackgrounds.java`), with:
//   -Dsvg.seed=<long>            common seed, the output is reproducible
//   -Dsvg.<theme>.seed=<long>    seed offset of one theme (its own grid)
//   -Dsvg.themes=blue,red,...    colour variants to write
//   -Dsvg.<theme>.<role>=#hex    colour overrides
//                                (role: base, deep, line, accent, spark)
//   -Dsvg.out=<dir>              output directory, one sub directory per theme
//   -Dsvg.debug=true             debug rendering (black background, red and blue meshes)
//   -Dsvg.force=true             rewrite everything even when nothing changed
//
// The stamp: generated/.stamp holds the SHA-256 of the svg.* properties and of this
// file. When it matches and the 5 pictures of every theme exist, nothing is written
// and the run costs a JVM startup only. `mvn clean` keeps the folder (see
// books/pom.xml), so a clean build reuses the pictures.
//
// Every file has its own Random seeded from svg.seed plus the theme offset, so the
// same seeds always give the same bytes.
//
// Output, per theme, in svg.out/<theme>/:
//   cover.svg           A4 page (210 x 297 mm), cover of the book: gradient + mesh
//   part.svg            A4 page, same gradient and glow as the cover but without the mesh
//   banner-hexagon.svg  21 x 9.4 cm, hexagon pattern (chapter banner)
//   banner-wave.svg     21 x 9.4 cm, sine waves and one Gaussian bump (outline banner)
//   banner-bands.svg    21 x 9.4 cm, slanted bands with progressive transparency
// =============================================================================
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GenerateSvgBackgrounds {

    static long svgSeed = Long.parseLong(System.getProperty("svg.seed", "2481"));
    static String svgThemes = System.getProperty("svg.themes", "blue,red,green");
    static Path svgOut = Path.of(System.getProperty("svg.out", "src/main/typst/common/assets/generated"));
    static boolean svgDebug = Boolean.getBoolean("svg.debug");

    static Map<String, Map<String, String>> svgDefaults = Map.of(
        "blue", Map.of("base", "#0e2a47", "deep", "#00459e", "line", "#03305d", "accent", "#3379c2", "spark", "#3fe0a0"),
        "red", Map.of("base", "#2b070d", "deep", "#4a0b16", "line", "#5a0b18", "accent", "#c8102e", "spark", "#ffb020"),
        "green", Map.of("base", "#0c3323", "deep", "#123a2b", "line", "#06402a", "accent", "#34a86c", "spark", "#7fe0c0"));

    static String svgColor(String theme, String role) {
        return System.getProperty("svg." + theme + "." + role, svgDefaults.get(theme).get(role));
    }

    static long svgThemeSeed(String theme) {
        return svgSeed + Long.parseLong(System.getProperty("svg." + theme + ".seed", "0"));
    }

    static String svgNum(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    // Blends a "#rrggbb" colour towards white (t = 0 keeps it, t = 1 gives white).
    static String svgLighten(String hex, double t) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        r = (int) Math.round(r + (255 - r) * t);
        g = (int) Math.round(g + (255 - g) * t);
        b = (int) Math.round(b + (255 - b) * t);
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    static String svgHex(double cx, double cy, double r, double angleOffset) {
        var sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            double a = StrictMath.toRadians(60 * i + angleOffset);
            sb.append(i == 0 ? "M" : "L")
              .append(svgNum(cx + r * StrictMath.cos(a))).append(' ')
              .append(svgNum(cy + r * StrictMath.sin(a)));
        }
        return sb.append("Z").toString();
    }

    static String svgSmoothPath(double[] xs, double[] ys) {
        var sb = new StringBuilder();
        sb.append("M").append(svgNum(xs[0])).append(' ').append(svgNum(ys[0]));
        for (int i = 0; i < xs.length - 1; i++) {
            int p0 = Math.max(i - 1, 0);
            int p3 = Math.min(i + 2, xs.length - 1);
            sb.append(" C")
              .append(svgNum(xs[i] + (xs[i + 1] - xs[p0]) / 6)).append(' ')
              .append(svgNum(ys[i] + (ys[i + 1] - ys[p0]) / 6)).append(' ')
              .append(svgNum(xs[i + 1] - (xs[p3] - xs[i]) / 6)).append(' ')
              .append(svgNum(ys[i + 1] - (ys[p3] - ys[i]) / 6)).append(' ')
              .append(svgNum(xs[i + 1])).append(' ').append(svgNum(ys[i + 1]));
        }
        return sb.toString();
    }

    static void svgPeaks(StringBuilder sb, Random rnd, double w, double h, String accent) {
        int count = 8;
        double rise = 92;
        double step = (w + rise) / count;
        for (int i = 0; i < count; i++) {
            boolean leansRight = (i % 2) == 0;
            double x = -rise + step * (i + 0.3 * rnd.nextDouble());
            double thick = 16 + 60 * rnd.nextDouble();
            double base = h + 4 * rnd.nextDouble();
            String d;
            if (leansRight) {
                d = "M" + svgNum(x) + " " + svgNum(base)
                  + " L" + svgNum(x + rise) + " " + svgNum(base - rise)
                  + " L" + svgNum(x + rise + thick) + " " + svgNum(base - rise)
                  + " L" + svgNum(x + thick) + " " + svgNum(base) + "Z";
            } else {
                d = "M" + svgNum(x) + " " + svgNum(base)
                  + " L" + svgNum(x + thick) + " " + svgNum(base)
                  + " L" + svgNum(x + thick - rise) + " " + svgNum(base - rise)
                  + " L" + svgNum(x - rise) + " " + svgNum(base - rise) + "Z";
            }
            sb.append("<path d=\"").append(d).append("\" fill=\"url(#").append(leansRight ? "tri" : "tri2").append(")\"/>\n");
        }
    }

    static void svgWaves(StringBuilder sb, Random rnd, double w, double h, String accent) {
        int n = 120;
        double start = -10;
        double end = w + 10;
        // Six curves: four sine waves, then two Gaussian bumps. The sines stay low and
        // interleave; the Gaussians rise smoothly into the upper part of the banner and
        // come back down without any cut, unlike a window function.
        double[] wavelengths = {80, 110, 170, 240};
        double[] crests = {0.12, 0.38, 0.62, 0.88};
        for (int i = 0; i < wavelengths.length; i++) {
            double amp = h * (0.20 + 0.10 * rnd.nextDouble());
            double freq = 2 * StrictMath.PI / (wavelengths[i] * (0.92 + 0.16 * rnd.nextDouble()));
            double crestX = w * crests[i] + (rnd.nextDouble() - 0.5) * w * 0.10;
            double phase = StrictMath.PI / 2 - freq * crestX;
            double cy = h * (0.66 + 0.08 * rnd.nextDouble());
            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int j = 0; j < n; j++) {
                xs[j] = start + (end - start) * j / (n - 1);
                ys[j] = cy + amp * StrictMath.sin(freq * xs[j] + phase);
            }
            sb.append("<path d=\"").append(svgSmoothPath(xs, ys)).append("\" fill=\"none\" stroke=\"").append(accent)
              .append("\" stroke-opacity=\"0.6\" stroke-width=\"0.35\"/>\n");
        }

        // One Gaussian bump, wide and tall: it rises above the visible top of the banner
        // (the image is shifted up by 3 cm in the book) and comes down smoothly on both
        // sides, which fills the upper area without any cut.
        double center = w * (0.75 + 0.06 * (rnd.nextDouble() - 0.5));
        double sigma = w * 0.13 * (0.9 + 0.2 * rnd.nextDouble());
        double base = h * (0.94 + 0.03 * rnd.nextDouble());
        double amp = h * (0.76 + 0.08 * rnd.nextDouble());
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int j = 0; j < n; j++) {
            double x = start + (end - start) * j / (n - 1);
            double dx = x - center;
            xs[j] = x;
            ys[j] = base - amp * StrictMath.exp(-(dx * dx) / (2 * sigma * sigma));
        }
        sb.append("<path d=\"").append(svgSmoothPath(xs, ys)).append("\" fill=\"none\" stroke=\"").append(accent)
          .append("\" stroke-opacity=\"0.6\" stroke-width=\"0.35\"/>\n");
    }

    static double svgEdge(double t, double w, double shift) {
        double u = 1 - t;
        return w * (0.05 + 0.80 * u + 0.18 * StrictMath.sin(StrictMath.PI * u)) + shift * w;
    }

    static boolean svgInMesh(double x, double y, double w, double h, double shift) {
        return x > svgEdge(y / h, w, shift);
    }

    static void svgBoundary(StringBuilder sb, double w, double h, double shift, String color) {
        var d = new StringBuilder();
        for (int i = 0; i <= 80; i++) {
            double t = i / 80.0;
            d.append(i == 0 ? "M" : "L").append(svgNum(svgEdge(t, w, shift))).append(' ').append(svgNum(t * h));
        }
        sb.append("<path d=\"").append(d).append("\" fill=\"none\" stroke=\"").append(color)
          .append("\" stroke-width=\"0.8\" stroke-dasharray=\"2 1\"/>\n");
    }

    static void svgMesh(StringBuilder sb, Random rnd, double w, double h, String accent, String spark, double shift, double opacity, boolean front) {
        double s = 13;
        double jitter = 4.5;
        int cols = (int) (w / s) + 3;
        int rows = (int) (h / s) + 3;
        double[][] xs = new double[rows][cols];
        double[][] ys = new double[rows][cols];
        boolean[][] on = new boolean[rows][cols];
        boolean[][] linked = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = -s + c * s + (rnd.nextDouble() - 0.5) * 2 * jitter;
                double y = -s + r * s + (rnd.nextDouble() - 0.5) * 2 * jitter;
                xs[r][c] = x;
                ys[r][c] = y;
                on[r][c] = svgInMesh(x, y, w, h, shift);
            }
        }
        int[][] links = {{0, 1}, {1, -1}, {1, 0}, {1, 1}};
        double[] levels = {0.10, 0.18, 0.28};
        String debugColor = front ? "#ff0000" : "#0000ff";
        var accentPaths = new StringBuilder[levels.length];
        var sparkPaths = new StringBuilder[levels.length];
        for (int i = 0; i < levels.length; i++) {
            accentPaths[i] = new StringBuilder();
            sparkPaths[i] = new StringBuilder();
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!on[r][c]) {
                    continue;
                }
                for (int[] d : links) {
                    int rr = r + d[0];
                    int cc = c + d[1];
                    if (rr < 0 || rr >= rows || cc < 0 || cc >= cols || !on[rr][cc]) {
                        continue;
                    }
                    boolean hot = rnd.nextDouble() < (front ? 0.25 : 0.10);
                    int level = svgDebug ? 0 : rnd.nextInt(levels.length);
                    linked[r][c] = true;
                    linked[rr][cc] = true;
                    (svgDebug ? accentPaths : (hot ? sparkPaths : accentPaths))[level]
                        .append("M").append(svgNum(xs[r][c])).append(' ').append(svgNum(ys[r][c]))
                        .append("L").append(svgNum(xs[rr][cc])).append(' ').append(svgNum(ys[rr][cc]));
                }
            }
        }
        for (int i = 0; i < levels.length; i++) {
            for (var pair : new Object[][] {{accentPaths[i], svgDebug ? debugColor : accent}, {sparkPaths[i], spark}}) {
                var d = (StringBuilder) pair[0];
                if (d.length() == 0) {
                    continue;
                }
                sb.append("<path d=\"").append(d).append("\" fill=\"none\" stroke=\"").append(pair[1])
                  .append("\" stroke-opacity=\"").append(svgNum(svgDebug ? 1.0 : opacity * levels[i]))
                  .append("\" stroke-width=\"").append(svgDebug ? "0.4" : "0.25").append("\"/>\n");
            }
        }
        var dots = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!on[r][c] || !linked[r][c]) {
                    continue;
                }
                double px = xs[r][c] / w;
                double sparkP = 0.01 + 0.07 * px;
                if (!svgDebug && rnd.nextDouble() < sparkP) {
                    sb.append("<circle cx=\"").append(svgNum(xs[r][c])).append("\" cy=\"").append(svgNum(ys[r][c]))
                      .append("\" r=\"2.4\" fill=\"url(#spark)\"/>\n");
                    sb.append("<circle cx=\"").append(svgNum(xs[r][c])).append("\" cy=\"").append(svgNum(ys[r][c]))
                      .append("\" r=\"0.4\" fill=\"").append(spark).append("\"/>\n");
                } else {
                    dots.append("M").append(svgNum(xs[r][c])).append(' ').append(svgNum(ys[r][c])).append("h0.01");
                }
            }
        }
        if (dots.length() > 0) {
            sb.append("<path d=\"").append(dots).append("\" fill=\"none\" stroke=\"").append(svgDebug ? debugColor : accent)
              .append("\" stroke-opacity=\"").append(svgNum(svgDebug ? 1.0 : opacity * 0.7))
              .append("\" stroke-width=\"").append(svgDebug ? "0.8" : "0.6").append("\" stroke-linecap=\"round\"/>\n");
        }
    }

    static String svgRender(String theme, String kind, double w, double h) {
        long offset = switch (kind) {
            case "cover" -> 1;
            case "banner" -> 2;
            case "bands" -> 4;
            case "part" -> 5;
            default -> 3;
        };
        Random rnd = new Random(svgThemeSeed(theme) + offset);

        String base = svgColor(theme, "base");
        String deep = svgColor(theme, "deep");
        String line = svgColor(theme, "line");
        String accent = svgColor(theme, "accent");
        String spark = svgColor(theme, "spark");

        double r = switch (kind) {
            case "cover" -> 7;
            case "banner" -> 5.5;
            default -> 6;
        };
        double fillP = 0.10;
        double strokeP = 0.35;
        String outline = kind.equals("banner")
            ? " stroke=\"" + accent + "\" stroke-opacity=\"0.65\""
            : " stroke=\"" + line + "\"";

        var sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(svgNum(w)).append("mm\" height=\"")
          .append(svgNum(h)).append("mm\" viewBox=\"0 0 ").append(svgNum(w)).append(' ').append(svgNum(h)).append("\">\n");
        sb.append("<defs>\n");
        sb.append("<linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">")
          .append("<stop offset=\"0\" stop-color=\"").append(deep).append("\"/>")
          .append("<stop offset=\"1\" stop-color=\"").append(base).append("\"/></linearGradient>\n");
        String glowColor = kind.equals("part") ? svgLighten(accent, 0.55) : accent;
        sb.append("<radialGradient id=\"glow\">")
          .append("<stop offset=\"0\" stop-color=\"").append(glowColor).append("\" stop-opacity=\"")
          .append(kind.equals("part") ? "0.55" : "0.45").append("\"/>")
          .append("<stop offset=\"1\" stop-color=\"").append(glowColor).append("\" stop-opacity=\"0\"/></radialGradient>\n");
        if (kind.equals("bands")) {
            sb.append("<linearGradient id=\"tri\" x1=\"0\" y1=\"1\" x2=\"1\" y2=\"0\">")
              .append("<stop offset=\"0\" stop-color=\"").append(accent).append("\" stop-opacity=\"0.35\"/>")
              .append("<stop offset=\"0.65\" stop-color=\"").append(accent).append("\" stop-opacity=\"0\"/></linearGradient>\n");
            sb.append("<linearGradient id=\"tri2\" x1=\"1\" y1=\"1\" x2=\"0\" y2=\"0\">")
              .append("<stop offset=\"0\" stop-color=\"").append(accent).append("\" stop-opacity=\"0.35\"/>")
              .append("<stop offset=\"0.65\" stop-color=\"").append(accent).append("\" stop-opacity=\"0\"/></linearGradient>\n");
        }
        if (kind.equals("cover")) {
            sb.append("<radialGradient id=\"spark\">")
              .append("<stop offset=\"0\" stop-color=\"").append(spark).append("\" stop-opacity=\"0.28\"/>")
              .append("<stop offset=\"1\" stop-color=\"").append(spark).append("\" stop-opacity=\"0\"/></radialGradient>\n");
        }
        sb.append("</defs>\n");
        boolean flat = kind.equals("banner") || kind.equals("bands");
        String fill = svgDebug && kind.equals("cover") ? "#000000" : (flat ? base : "url(#bg)");
        sb.append("<rect width=\"").append(svgNum(w)).append("\" height=\"").append(svgNum(h))
          .append("\" fill=\"").append(fill).append("\"/>\n");

        if (!flat && !svgDebug) {
            double gx;
            double gy;
            double gr;
            if (kind.equals("part")) {
                // Huge glow in the bottom right corner: it lights the area where the part
                // page prints its outline, so the text stays readable on the dark gradient.
                gx = w * 0.88;
                gy = h * 0.90;
                gr = Math.max(w, h) * 0.85;
            } else if (kind.equals("cover")) {
                gx = w * 0.82;
                gy = h * 0.55;
                gr = Math.max(w, h) * 0.45;
            } else {
                gx = w * rnd.nextDouble();
                gy = h * rnd.nextDouble();
                gr = Math.max(w, h) * 0.45;
            }
            sb.append("<circle cx=\"").append(svgNum(gx)).append("\" cy=\"").append(svgNum(gy))
              .append("\" r=\"").append(svgNum(gr)).append("\" fill=\"url(#glow)\"/>\n");
        }

        if (kind.equals("banner")) {
            double dy = r * Math.sqrt(3);
            for (int q = -1; q * 1.5 * r <= w + r; q++) {
                double cx = q * 1.5 * r;
                double oddShift = (q & 1) == 1 ? dy / 2 : 0;
                for (int row = -1; row * dy + oddShift <= h + dy; row++) {
                    double cy = row * dy + oddShift;
                    double roll = rnd.nextDouble();
                    String d = svgHex(cx, cy, r, 0);
                    if (roll < fillP) {
                        sb.append("<path d=\"").append(d).append("\" fill=\"").append(line).append("\" fill-opacity=\"0.9\"/>\n");
                    } else if (roll < fillP + strokeP) {
                        sb.append("<path d=\"").append(d).append("\" fill=\"none\"").append(outline)
                          .append(" stroke-width=\"0.3\"/>\n");
                    }
                }
            }
        }

        if (kind.equals("cover")) {
            // Variant in use: one single mesh, small dots on every vertex, a few glowing
            // dots, denser on the right where the pattern is fully visible.
            svgMesh(sb, rnd, w, h, accent, spark, -0.35, 1.0, true);

            // Variant with two meshes: dimmer one behind, shifted left, without glowing dots.
            // svgMesh(sb, rnd, w, h, accent, spark, -0.35, 0.85, false);
            // svgMesh(sb, rnd, w, h, accent, spark, -0.09, 1.0, true);

            if (svgDebug) {
                svgBoundary(sb, w, h, -0.35, "#ff0000");
            }
        }
        if (kind.equals("wave")) {
            svgWaves(sb, rnd, w, h, accent);
        }
        if (kind.equals("bands")) {
            svgPeaks(sb, rnd, w, h, accent);
        }

        return sb.append("</svg>\n").toString();
    }

    public static void main(String[] args) throws Exception {
        String[] svgFiles = {"cover.svg", "part.svg", "banner-hexagon.svg", "banner-wave.svg", "banner-bands.svg"};
        var svgThemeList = new java.util.ArrayList<String>();
        for (String t : svgThemes.split(",")) {
            svgThemeList.add(t.trim());
        }

        // Fingerprint of everything that drives the output: the svg.* properties and the
        // generator itself. When it matches the stored stamp and every expected file
        // exists, nothing is written, so a rebuild only pays the JVM startup.
        // `-Dsvg.force=true` bypasses the check.
        var svgFp = new StringBuilder();
        var svgNames = new java.util.ArrayList<>(System.getProperties().stringPropertyNames());
        java.util.Collections.sort(svgNames);
        for (String n : svgNames) {
            // svg.script and svg.out locate the inputs and the output, svg.force is a
            // one-shot switch: none of them describes the pictures.
            if (n.startsWith("svg.") && !n.equals("svg.script") && !n.equals("svg.out") && !n.equals("svg.force")) {
                svgFp.append(n).append('=').append(System.getProperty(n)).append('\n');
            }
        }
        String svgScriptPath = System.getProperty("svg.script", "");
        if (!svgScriptPath.isEmpty() && Files.exists(Path.of(svgScriptPath))) {
            svgFp.append("script=").append(Files.readString(Path.of(svgScriptPath), StandardCharsets.UTF_8));
        }
        String svgStamp = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(svgFp.toString().getBytes(StandardCharsets.UTF_8)));
        Path svgStampFile = svgOut.resolve(".stamp");
        boolean svgStampOk = Files.exists(svgStampFile) && Files.readString(svgStampFile, StandardCharsets.UTF_8).trim().equals(svgStamp);
        boolean svgUpToDate = !Boolean.getBoolean("svg.force") && svgStampOk;
        if (svgUpToDate) {
            for (String t : svgThemeList) {
                for (String f : svgFiles) {
                    if (!Files.exists(svgOut.resolve(t).resolve(f))) {
                        svgUpToDate = false;
                    }
                }
            }
        }

        if (svgUpToDate) {
            System.out.println("svg backgrounds up to date in " + svgOut);
        } else {
            for (String theme : svgThemeList) {
                Path dir = svgOut.resolve(theme);
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("cover.svg"), svgRender(theme, "cover", 210, 297), StandardCharsets.UTF_8);
                Files.writeString(dir.resolve("part.svg"), svgRender(theme, "part", 210, 297), StandardCharsets.UTF_8);
                Files.writeString(dir.resolve("banner-hexagon.svg"), svgRender(theme, "banner", 210, 94), StandardCharsets.UTF_8);
                Files.writeString(dir.resolve("banner-wave.svg"), svgRender(theme, "wave", 210, 94), StandardCharsets.UTF_8);
                Files.writeString(dir.resolve("banner-bands.svg"), svgRender(theme, "bands", 210, 94), StandardCharsets.UTF_8);
                System.out.println("generated " + dir);
            }
            Files.writeString(svgStampFile, svgStamp + "\n", StandardCharsets.UTF_8);
        }
    }
}
