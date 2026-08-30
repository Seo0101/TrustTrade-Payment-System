package org.example.trusttrade.item.dto.request;

import java.util.List;

public interface ItemImageAttachable {
    void setMainImage(String mainImage);
    void setSubImages(List<String> subImages);
}