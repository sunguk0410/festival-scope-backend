package likelion.festivalscope.document.extractor;

import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
public class PdfTextExtractor implements DocumentTextExtractor {
    @Override
    public boolean supports(String contentType, String fileName) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }

    @Override
    public String extract(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document)
                    .replace("\u0000", "")
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\\n\\n")
                    .trim();
            if (text.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "PDF에서 텍스트를 추출할 수 없습니다. 텍스트 기반 PDF 파일을 업로드해주세요.");
            }
            return text;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            log.warn("PDF text extraction failed: fileName={}", file.getOriginalFilename(), exception);
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "PDF 텍스트 추출에 실패했습니다. 텍스트 기반 PDF 파일인지 확인해주세요.");
        }
    }
}
