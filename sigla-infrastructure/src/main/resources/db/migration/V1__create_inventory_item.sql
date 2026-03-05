CREATE TABLE IF NOT EXISTS inventory_item (
    sku VARCHAR(40) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL
);
