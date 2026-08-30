package org.example.trusttrade.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class StoredImage {
    private final String url;
    private final Path absolutePath;
}

/**
 * 업로드 결과 DTO (URL과 실제 파일 경로)
 */