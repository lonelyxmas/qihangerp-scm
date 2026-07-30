package cn.qihang.ai.assistant.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
            "txt", "html", "htm", "xml", "csv", "rtf", "odt", "ods", "odp"
    );

    private static final Set<String> MEDIA_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp",
            "mp3", "mp4", "avi", "mov", "wav", "flac",
            "zip", "rar", "7z", "tar", "gz",
            "exe", "dll", "so", "bin"
    );

    private final Tika tika;

    public DocumentParserService() {
        this.tika = new Tika();
    }

    public boolean isSupported(String filename) {
        if (filename == null) return false;
        return SUPPORTED_EXTENSIONS.contains(getExtension(filename));
    }

    public boolean isMediaFile(String filename) {
        if (filename == null) return false;
        return MEDIA_EXTENSIONS.contains(getExtension(filename));
    }

    public String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    public String parseToMarkdown(byte[] data, String filename) {
        String rawText = extractText(data, filename);
        if (rawText == null || rawText.isBlank()) {
            return "# " + filename + "\n\n*锛堟枃妗ｅ唴瀹逛负绌烘垨鏃犳硶瑙ｆ瀽锛?";
        }
        return "# " + filename + "\n\n" + rawText;
    }

    public String extractText(byte[] data, String filename) {
        try (InputStream is = new ByteArrayInputStream(data)) {
            Metadata metadata = new Metadata();
            if (filename != null) {
                metadata.set("resourceName", filename);
            }
            return tika.parseToString(is, metadata);
        } catch (TikaException | IOException e) {
            log.warn("Tika parse failed for {}: {}", filename, e.getMessage());
            return fallbackExtractText(data, filename);
        }
    }

    private String fallbackExtractText(byte[] data, String filename) {
        String ext = getExtension(filename);
        if ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext) || "xml".equals(ext) || "csv".equals(ext)) {
            return new String(data, java.nio.charset.StandardCharsets.UTF_8);
        }
        try (InputStream is = new ByteArrayInputStream(data);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            if (filename != null) {
                metadata.set("resourceName", filename);
            }
            ToHTMLContentHandler handler = new ToHTMLContentHandler(os, "UTF-8");
            parser.parse(is, handler, metadata, new ParseContext());
            String html = os.toString("UTF-8");
            return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        } catch (Exception e2) {
            log.warn("Fallback parse also failed for {}: {}", filename, e2.getMessage());
            return "*锛堟棤娉曡В鏋愭鏂囨。鏍煎紡锛?";
        }
    }
}