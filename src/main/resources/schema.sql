SET
FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS history_records;
DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS auction_sessions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;

SET
FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) UNIQUE NOT NULL,
    password   VARCHAR(255)       NOT NULL,
    full_name  VARCHAR(100)       NOT NULL,
    email      VARCHAR(100) UNIQUE,
    assets     INT       DEFAULT 0,
    role       ENUM('ADMIN','SELLER','BIDDER') DEFAULT 'BIDDER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE items
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    seller_id      INT          NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    category       VARCHAR(50),
    starting_price INT          NOT NULL CHECK (starting_price > 0),
    step_price     INT          NOT NULL CHECK (step_price > 0),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status         ENUM('AVAILABLE','AUCTIONING','SOLD'),
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE auction_sessions
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    item_id        INT      NOT NULL,
    UNIQUE (item_id),
    seller_id      INT      NOT NULL,
    winner_id      INT NULL,
    status         ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELLED') DEFAULT 'OPEN',
    start_time     DATETIME  DEFAULT CURRENT_TIMESTAMP,
    end_time       DATETIME NOT NULL,
    highest_bid    INT       DEFAULT 0,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    extended_count INT       DEFAULT 0,
    version        INT       DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE bids
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    session_id  INT NOT NULL,
    user_id     INT NOT NULL,
    bid_amount  INT NOT NULL,
    bid_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN   DEFAULT FALSE,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES auction_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE auto_bids
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    session_id       INT NOT NULL,
    user_id          INT NOT NULL,
    UNIQUE (session_id, user_id),
    max_bid          INT NOT NULL,
    increment_amount INT NOT NULL,
    is_active        BOOLEAN   DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES auction_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_bids_session ON bids (session_id);
CREATE INDEX idx_bids_user ON bids (user_id);
CREATE INDEX idx_auction_status ON auction_sessions (status);
CREATE INDEX idx_session_end_time ON auction_sessions (end_time);
CREATE INDEX idx_auto_bid_session ON auto_bids (session_id);
CREATE INDEX idx_history_session ON history_records (session_id);