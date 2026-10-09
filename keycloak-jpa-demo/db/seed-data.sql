-- ============================================================================
-- seed-data.sql
-- Run after schema.sql. Populates enough rows to demo all relationship types.
-- ============================================================================
USE ecommerce_demo;

-- Categories (self-referencing 1:many via parent_category_id)
INSERT INTO categories (category_id, category_name, parent_category_id, description) VALUES
(1, 'Electronics', NULL, 'Devices and gadgets'),
(2, 'Laptops', 1, 'Portable computers'),
(3, 'Audio', 1, 'Headphones and speakers'),
(4, 'Home & Kitchen', NULL, 'Household goods'),
(5, 'Cookware', 4, 'Pots, pans, and bakeware');

-- Tags (independent entities, many:many with products)
INSERT INTO tags (tag_id, tag_name) VALUES
(1, 'bestseller'),
(2, 'clearance'),
(3, 'new-arrival'),
(4, 'eco-friendly'),
(5, 'premium');

-- Products (many:one to categories)
INSERT INTO products (product_id, sku, product_name, description, unit_price, stock_quantity, category_id, active) VALUES
(1, 'ELEC-LAP-001', 'AeroBook 14 Laptop', '14-inch ultrabook, 16GB RAM, 512GB SSD', 1099.00, 40, 2, 1),
(2, 'ELEC-LAP-002', 'AeroBook 14 Pro', '14-inch ultrabook, 32GB RAM, 1TB SSD', 1599.00, 25, 2, 1),
(3, 'ELEC-AUD-001', 'SkyBuds Wireless Earbuds', 'Active noise cancelling earbuds', 149.00, 120, 3, 1),
(4, 'ELEC-AUD-002', 'ThunderBox Bluetooth Speaker', 'Portable speaker, 20h battery', 79.00, 80, 3, 1),
(5, 'HOME-CK-001', 'IronClad 10-inch Skillet', 'Pre-seasoned cast iron skillet', 39.00, 60, 5, 1),
(6, 'HOME-CK-002', 'IronClad 3-Piece Cookware Set', 'Cast iron skillet, saucepan, dutch oven', 129.00, 30, 5, 1);

-- product_tags (MANY-TO-MANY join rows)
INSERT INTO product_tags (product_id, tag_id) VALUES
(1, 1), (1, 3),
(2, 3), (2, 5),
(3, 1), (3, 4),
(4, 2),
(5, 4), (5, 1),
(6, 4), (6, 5);

-- Customers
INSERT INTO customers (customer_id, email, first_name, last_name, keycloak_sub) VALUES
(1, 'avery.chen@example.com', 'Avery', 'Chen', NULL),
(2, 'devon.ramirez@example.com', 'Devon', 'Ramirez', NULL),
(3, 'priya.natarajan@example.com', 'Priya', 'Natarajan', NULL);

-- customer_profiles (ONE-TO-ONE, exactly one row per customer)
INSERT INTO customer_profiles
(customer_id, phone_number, date_of_birth, loyalty_tier, marketing_opt_in, shipping_address_l1, city, state_code, postal_code, country_code) VALUES
(1, '312-555-0101', '1990-04-12', 'GOLD',   1, '100 Wacker Dr',        'Chicago',   'IL', '60601', 'USA'),
(2, '630-555-0110', '1988-11-02', 'SILVER', 0, '55 Naperville Rd',     'Naperville','IL', '60540', 'USA'),
(3, '773-555-0199', '1995-07-23', 'BRONZE', 1, '900 N Michigan Ave',   'Chicago',   'IL', '60611', 'USA');

-- Orders (ONE-TO-MANY: customer -> orders)
INSERT INTO orders (order_id, order_number, customer_id, order_status, order_total, placed_at) VALUES
(1, 'ORD-2026-0001', 1, 'SHIPPED',   1248.00, '2026-08-01 10:15:00'),
(2, 'ORD-2026-0002', 1, 'DELIVERED', 79.00,   '2026-08-15 14:02:00'),
(3, 'ORD-2026-0003', 2, 'CREATED',   149.00,  '2026-09-01 09:30:00'),
(4, 'ORD-2026-0004', 3, 'DELIVERED', 129.00,  '2026-09-10 17:45:00');

-- Payments (ONE-TO-ONE: order -> payment)
INSERT INTO payments (order_id, payment_method, payment_status, amount_charged, transaction_ref, processed_at) VALUES
(1, 'CREDIT_CARD', 'CAPTURED', 1248.00, 'TXN-90001', '2026-08-01 10:16:00'),
(2, 'CREDIT_CARD', 'CAPTURED', 79.00,   'TXN-90002', '2026-08-15 14:03:00'),
(3, 'PAYPAL',      'PENDING',  149.00,  'TXN-90003', NULL),
(4, 'DEBIT_CARD',  'CAPTURED', 129.00,  'TXN-90004', '2026-09-10 17:46:00');

-- Order items (ONE-TO-MANY: order -> order_items, MANY-TO-ONE: order_items -> product)
INSERT INTO order_items (order_id, product_id, quantity, unit_price_at_purchase, line_total) VALUES
(1, 1, 1, 1099.00, 1099.00),
(1, 3, 1, 149.00,  149.00),
(2, 4, 1, 79.00,   79.00),
(3, 3, 1, 149.00,  149.00),
(4, 6, 1, 129.00,  129.00);

-- Sanity check: order_total should equal SUM(line_total) per order.
-- (Left as an exercise / assertion in the JPA service layer demo.)

