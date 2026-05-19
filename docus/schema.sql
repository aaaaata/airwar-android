-- AirWar Android 后端数据库初始化脚本
-- MySQL 8.x
CREATE DATABASE IF NOT EXISTS airwar
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE airwar;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  avatar_id INT NOT NULL DEFAULT 1,
  coins INT NOT NULL DEFAULT 0,
  selected_skin_id INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_date DATE NULL,
  INDEX idx_users_username_password (username, password)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_skins (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  skin_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_skin (user_id, skin_id),
  INDEX idx_user_skins_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scores (
  score_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL,
  avatar_id INT NOT NULL DEFAULT 1,
  difficulty VARCHAR(16) NOT NULL,
  score INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_scores_rank (difficulty, score DESC, created_at ASC, score_id ASC),
  INDEX idx_scores_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pk_room (
  room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_code VARCHAR(16) NOT NULL UNIQUE,
  room_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',

  player1_user_id BIGINT NOT NULL,
  player1_username VARCHAR(64) NOT NULL,
  player1_avatar_id INT NOT NULL DEFAULT 1,
  player1_ready TINYINT(1) NOT NULL DEFAULT 0,
  player1_score INT NOT NULL DEFAULT 0,
  player1_finished TINYINT(1) NOT NULL DEFAULT 0,

  player2_user_id BIGINT NULL,
  player2_username VARCHAR(64) NULL,
  player2_avatar_id INT NULL DEFAULT 1,
  player2_ready TINYINT(1) NOT NULL DEFAULT 0,
  player2_score INT NOT NULL DEFAULT 0,
  player2_finished TINYINT(1) NOT NULL DEFAULT 0,

  winner_user_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at DATETIME NULL,
  ended_at DATETIME NULL,

  INDEX idx_pk_room_code (room_code),
  INDEX idx_pk_room_status_created (room_status, created_at),
  INDEX idx_pk_room_players (player1_user_id, player2_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
