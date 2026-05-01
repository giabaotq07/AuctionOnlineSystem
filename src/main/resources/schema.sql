SET FOREIGN_KEY_CHECKS = 0; -- Tắt kiểm tra khóa ngoại để xóa không bị lỗi

DROP TABLE IF EXISTS history_records;
DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS auction_sessions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1; -- Bật lại kiểm tra khóa ngoại

-- Sau đó mới chạy đoạn script CREATE TABLE của bạn
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    assets DOUBLE DEFAULT 0,
    role VARCHAR(20) DEFAULT 'BIDDER'
);
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    starting_price DOUBLE NOT NULL,
    step_price DOUBLE NOT NULL,
    type VARCHAR(30) NOT NULL
);
CREATE TABLE IF NOT EXISTS auction_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    seller_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    end_time DATETIME NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS history_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    message VARCHAR(255),
    time DATETIME NOT NULL,
    FOREIGN KEY (session_id) REFERENCES auction_sessions(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    bid_amount DOUBLE NOT NULL,
    time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES auction_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
