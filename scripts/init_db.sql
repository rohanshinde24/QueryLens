-- QueryLens Sample Database Schema
-- This creates sample tables for testing query analysis

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    age INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2),
    category VARCHAR(100),
    stock_quantity INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL
);

-- Insert sample data
INSERT INTO users (name, email, age, created_at) VALUES
    ('John Doe', 'john@example.com', 30, '2023-01-15'),
    ('Jane Smith', 'jane@example.com', 25, '2023-02-20'),
    ('Bob Johnson', 'bob@example.com', 35, '2023-03-10'),
    ('Alice Williams', 'alice@example.com', 28, '2023-04-05'),
    ('Charlie Brown', 'charlie@example.com', 42, '2023-05-12');

INSERT INTO products (name, description, price, category, stock_quantity) VALUES
    ('Laptop', 'High-performance laptop', 1299.99, 'Electronics', 50),
    ('Mouse', 'Wireless mouse', 29.99, 'Electronics', 200),
    ('Keyboard', 'Mechanical keyboard', 89.99, 'Electronics', 150),
    ('Monitor', '27-inch 4K monitor', 399.99, 'Electronics', 75),
    ('Desk Chair', 'Ergonomic office chair', 249.99, 'Furniture', 30);

INSERT INTO orders (user_id, order_date, total_amount, status) VALUES
    (1, '2023-06-01', 1329.98, 'completed'),
    (2, '2023-06-05', 89.99, 'completed'),
    (3, '2023-06-10', 429.98, 'pending'),
    (1, '2023-06-15', 249.99, 'completed'),
    (4, '2023-06-20', 1299.99, 'shipped');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
    (1, 1, 1, 1299.99),
    (1, 2, 1, 29.99),
    (2, 3, 1, 89.99),
    (3, 4, 1, 399.99),
    (3, 2, 1, 29.99),
    (4, 5, 1, 249.99),
    (5, 1, 1, 1299.99);

-- Create some indexes for demonstration (intentionally missing some for testing)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Note: Intentionally missing indexes on:
-- - users.age (for testing missing index detection)
-- - users.created_at (for testing non-sargable predicates)
-- - orders.status (for testing sequential scan detection)

COMMENT ON TABLE users IS 'Sample users table for QueryLens testing';
COMMENT ON TABLE orders IS 'Sample orders table for QueryLens testing';
COMMENT ON TABLE products IS 'Sample products table for QueryLens testing';

