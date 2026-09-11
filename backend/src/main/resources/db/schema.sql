CREATE TABLE usage_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(32)  NOT NULL,
    model           VARCHAR(64)  NOT NULL,
    input_tokens    INT UNSIGNED DEFAULT 0,
    output_tokens   INT UNSIGNED DEFAULT 0,
    total_tokens    INT UNSIGNED DEFAULT 0,
    duration_ms     INT UNSIGNED DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_created_at (created_at),
    KEY idx_provider_model (provider, model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='每次 AI 调用的使用记录，不保存 prompt/response 内容';
