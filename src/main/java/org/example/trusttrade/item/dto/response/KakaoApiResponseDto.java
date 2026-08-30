package org.example.trusttrade.item.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.trusttrade.item.dto.request.DocumentDto;
import org.example.trusttrade.item.dto.request.MetaDto;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class KakaoApiResponseDto {

    @JsonProperty("meta")
    private MetaDto metaDto;

    @JsonProperty("documents")
    private List<DocumentDto> documentList;
}