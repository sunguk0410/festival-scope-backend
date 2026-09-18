package likelion.festivalscope.document.service;

import likelion.festivalscope.document.extractor.DocumentTextExtractor;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentTextExtractionService {
    private final List<DocumentTextExtractor> extractors;

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "업로드할 PDF 파일이 없습니다.");
        }
        DocumentTextExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(file.getContentType(), file.getOriginalFilename()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "현재는 PDF 파일만 지원합니다."));
        return extractor.extract(file);
    }
}
