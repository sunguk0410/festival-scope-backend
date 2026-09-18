package likelion.festivalscope.document.extractor;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentTextExtractor {
    boolean supports(String contentType, String fileName);

    String extract(MultipartFile file);
}
