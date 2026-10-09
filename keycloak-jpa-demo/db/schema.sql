-- ============================================================================
-- schema.sql
-- Ecommerce reference schema for Spring Data JPA relationship demo
-- MySQL 8.0
-- DBA-owned DDL. Spring Boot runs with ddl-auto: validate against this schema.
--
-- Relationship map (for the YouTube walkthrough):
--   ONE-TO-ONE   : customers          <-> customer_profiles   (customer_profiles.customer_id is UNIQUE FK)
--   ONE-TO-MANY  : customers          ->  orders              (orders.customer_id FK, no unique constraint)
--   ONE-TO-MANY  : categories         ->  products             (products.category_id FK)
--   ONE-TO-MANY  : orders             ->  order_items          (order_items.order_id FK)
--   MANY-TO-MANY : products          <-> tags                  (product_tags join table)
--   MANY-TO-ONE  : order_items       ->  products               (order_items.product_id FK, the "many" side)
-- ============================================================================

DROP DATABASE IF EXISTS ecommerce_demo;
CREATE DATABASE ecommerce_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce_demo;

-- ----------------------------------------------------------------------------
-- customers  (parent of the 1:1 with customer_profiles and 1:many with orders)
-- ----------------------------------------------------------------------------
CREATE TABLE customers (
    customer_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    keycloak_sub    VARCHAR(64)  NULL COMMENT 'Keycloak user "sub" claim, linked after first broker login',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_keycloak_sub UNIQUE (keycloak_sub)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- customer_profiles  (ONE-TO-ONE child: PK == FK pattern, one row per customer)
-- ----------------------------------------------------------------------------
CREATE TABLE customer_profiles (
    profile_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id         BIGINT UNSIGNED NOT NULL,
    phone_number        VARCHAR(30)  NULL,
    date_of_birth       DATE         NULL,
    loyalty_tier        VARCHAR(20)  NOT NULL DEFAULT 'BRONZE',
    marketing_opt_in    TINYINT(1)   NOT NULL DEFAULT 0,
    shipping_address_l1 VARCHAR(255) NULL,
    shipping_address_l2 VARCHAR(255) NULL,
    city                VARCHAR(120) NULL,
    state_code          VARCHAR(10)  NULL,
    postal_code         VARCHAR(20)  NULL,
    country_code        VARCHAR(3)   NULL DEFAULT 'USA',
    CONSTRAINT uq_customer_profiles_customer UNIQUE (customer_id),
    CONSTRAINT fk_customer_profiles_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- categories  (parent of the 1:many with products)
-- ----------------------------------------------------------------------------
CREATE TABLE categories (
    category_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(120) NOT NULL,
    parent_category_id BIGINT UNSIGNED NULL,
    description     VARCHAR(500) NULL,
    CONSTRAINT uq_categories_name UNIQUE (category_name),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_category_id) REFERENCES categories(category_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- products  (many:one to categories, many:many to tags)
-- ----------------------------------------------------------------------------
CREATE TABLE products (
    product_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sku             VARCHAR(64)   NOT NULL,
    product_name    VARCHAR(255)  NOT NULL,
    description     TEXT          NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    stock_quantity  INT           NOT NULL DEFAULT 0,
    category_id     BIGINT UNSIGNED NOT NULL,
    active          TINYINT(1)    NOT NULL DEFAULT 1,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- tags  (the "other side" of the products <-> tags many:many)
-- ----------------------------------------------------------------------------
CREATE TABLE tags (
    tag_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tag_name    VARCHAR(60) NOT NULL,
    CONSTRAINT uq_tags_name UNIQUE (tag_name)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- product_tags  (MANY-TO-MANY join table, pure association, composite PK)
-- ----------------------------------------------------------------------------
CREATE TABLE product_tags (
    product_id  BIGINT UNSIGNED NOT NULL,
    tag_id      BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    CONSTRAINT fk_product_tags_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- orders  (many:one to customers, parent of 1:many order_items,
--          ONE-TO-ONE with payments)
-- ----------------------------------------------------------------------------
CREATE TABLE orders (
    order_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_number    VARCHAR(40)   NOT NULL,
    customer_id     BIGINT UNSIGNED NOT NULL,
    order_status    VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    order_total     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    placed_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- payments  (ONE-TO-ONE child of orders — one payment record per order)
-- ----------------------------------------------------------------------------
CREATE TABLE payments (
    payment_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT UNSIGNED NOT NULL,
    payment_method  VARCHAR(30)   NOT NULL,
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    amount_charged  DECIMAL(12,2) NOT NULL,
    transaction_ref VARCHAR(80)   NULL,
    processed_at    DATETIME(6)   NULL,
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- order_items  (ONE-TO-MANY child of orders, MANY-TO-ONE to products;
--               order_items is the classic "association entity" pattern —
--               a many:many between orders and products carrying extra
--               columns (quantity, unit_price_at_purchase) modeled as two
--               1:many / many:one relationships instead of a raw join table)
-- ----------------------------------------------------------------------------
CREATE TABLE order_items (
    order_item_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT UNSIGNED NOT NULL,
    product_id          BIGINT UNSIGNED NOT NULL,
    quantity             INT           NOT NULL,
    unit_price_at_purchase DECIMAL(12,2) NOT NULL,
    line_total           DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_products_category_id ON products(category_id);
