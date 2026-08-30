package org.example.trusttrade.item.dto.request;

import lombok.Getter;
import org.example.trusttrade.item.dto.request.ItemImageAttachable;

import java.util.List;

@Getter
public abstract  class AbstractItemImageDto implements ItemImageAttachable {
    protected String mainImage;
    protected List<String> subImages;

    @Override
    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    @Override
    public void setSubImages(List<String> subImages) {
        this.subImages = subImages;
    }


}