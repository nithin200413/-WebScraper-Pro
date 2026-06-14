-- ════════════════════════════════════════════════
--  WebScraper Pro — MySQL schema (for XAMPP)
--  Run this in phpMyAdmin (http://localhost/phpmyadmin)
--  or via:  mysql -u root < schema.sql
-- ════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS webscraper
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE webscraper;

CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(190)  NOT NULL UNIQUE,
  password_hash VARCHAR(255)  NOT NULL,
  created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Optional: store scrape history per user
CREATE TABLE IF NOT EXISTS scrape_history (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  url         VARCHAR(500) NOT NULL,
  title       VARCHAR(500),
  link_count  INT DEFAULT 0,
  image_count INT DEFAULT 0,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
