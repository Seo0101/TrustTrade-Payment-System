
ALTER TABLE product
    ADD COLUMN reserved_buyer_id BINARY(16);

ALTER TABLE product
    ADD CONSTRAINT fk_product_reserved_buyer
        FOREIGN KEY (reserved_buyer_id)
            REFERENCES users(id);