// =============================================================================
// Renders the first page of each book PDF as a JPEG, for the Docsify site.
//
// Launched by books/pom.xml (exec-maven-plugin, prepare-package phase) in
// source-file mode (`java -cp <pdfbox-libs> GenerateBookCovers.java`), with:
//   -Dcover.kurzweil.pdf=<file>   input PDF of the Kurzweil book
//   -Dcover.kurzweil.jpg=<file>   output JPEG (first page of the PDF above)
//   -Dcover.se-02.pdf=<file>      input PDF of the SE-02 book
//   -Dcover.se-02.jpg=<file>      output JPEG
//   -Dcover.akai.pdf=<file>       input PDF of the AKAI book
//   -Dcover.akai.jpg=<file>       output JPEG
//   -Dcover.ds-330.pdf=<file>     input PDF of the DS-330 book
//   -Dcover.ds-330.jpg=<file>     output JPEG
//   -Dcover.dpi=<float>           render resolution (default 150)
//   -Dcover.quality=<float>       JPEG quality 0..1 (default 0.85)
//   -Dcover.max-width=<int>       cap the rendered width in pixels (default 800)
//
// The output is written straight into site/docs/assets/, where midi-sysex.md
// references it, so the Docsify site always shows the covers produced by the
// current books. The PDFs are A4 portrait: at 150 dpi the first page is about
// 1240 x 1753 pixels, then it is scaled down to cover.max-width (800 px), which
// keeps the thumbnails light while staying sharp enough to zoom.
//
// Unlike the SVG generator, there is no stamp: the PDFs are rebuilt on every
// `mvn clean package`, so the covers are rendered again too. The render is
// deterministic for a given PDF and dpi, so an unchanged book produces the
// same JPEG bytes.
// =============================================================================
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class GenerateBookCovers {

    // One entry per book: the property names that locate its input PDF and its
    // output JPEG. Kept together so adding or renaming a book is a single edit.
    record Book(String pdf, String jpg) {}

    static final Book[] BOOKS = {
        new Book("cover.kurzweil.pdf", "cover.kurzweil.jpg"),
        new Book("cover.se-02.pdf", "cover.se-02.jpg"),
        new Book("cover.akai.pdf", "cover.akai.jpg"),
        new Book("cover.ds-330.pdf", "cover.ds-330.jpg"),
    };

    public static void main(String[] args) throws Exception {
        float dpi = Float.parseFloat(System.getProperty("cover.dpi", "150"));
        float quality = Float.parseFloat(System.getProperty("cover.quality", "0.85"));
        int maxWidth = Integer.parseInt(System.getProperty("cover.max-width", "800"));
        for (Book book : BOOKS) {
            Path pdf = Path.of(System.getProperty(book.pdf()));
            Path jpg = Path.of(System.getProperty(book.jpg()));
            renderFirstPage(pdf, jpg, dpi, quality, maxWidth);
        }
    }

    static void renderFirstPage(Path pdf, Path jpg, float dpi, float quality, int maxWidth) throws IOException {
        Files.createDirectories(jpg.toAbsolutePath().getParent());
        BufferedImage image;
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            image = new PDFRenderer(document).renderImageWithDPI(0, dpi);
        }
        if (image.getWidth() > maxWidth) {
            image = scaleToWidth(image, maxWidth);
        }
        writeJpeg(image, jpg, quality);
        System.out.printf(Locale.ROOT, "generated %s (%dx%d, max width %d px)%n",
            jpg, image.getWidth(), image.getHeight(), maxWidth);
    }

    // Scales down to maxWidth, preserving the aspect ratio. Rendered at dpi and
    // then downsampled with bicubic interpolation, so the text stays clean.
    static BufferedImage scaleToWidth(BufferedImage image, int maxWidth) {
        int height = (int) Math.round(image.getHeight() * (double) maxWidth / image.getWidth());
        BufferedImage scaled = new BufferedImage(maxWidth, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, maxWidth, height, null);
        g.dispose();
        return scaled;
    }

    // Encodes to a byte array first, then Files.write: writing straight into a
    // FileImageOutputStream would not truncate an existing, larger file, leaving
    // stale trailing bytes behind the JPEG end marker and bloating the file.
    static void writeJpeg(BufferedImage image, Path out, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        Files.write(out, bytes.toByteArray());
    }
}
