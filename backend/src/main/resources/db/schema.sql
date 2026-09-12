CREATE TABLE usage_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(32)  NOT NULL,
    model           VARCHAR(64)  NOT NULL,
    input_tokens    INT UNSIGNED DEFAULT 0,
    cache_hit_tokens  INT UNSIGNED DEFAULT 0,   -- 命中缓存的输入 token，无缓存概念时存 0
    cache_miss_tokens INT UNSIGNED DEFAULT 0,   -- 未命中缓存的输入 token，无缓存概念时存 0
    output_tokens   INT UNSIGNED DEFAULT 0,
    total_tokens    INT UNSIGNED DEFAULT 0,
    duration_ms     INT UNSIGNED DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 0,
    http_status     INT NULL,                    -- 上游返回的 HTTP 状态码，成功路径也记录
    error_message   VARCHAR(512) NULL,           -- 失败时的错误摘要，成功时为 NULL
    endpoint        VARCHAR(128) NULL,           -- 请求的上游路径，如 /v1/chat/completions
    stream          TINYINT NOT NULL DEFAULT 0,  -- 0=非流式 1=流式
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_created_at (created_at),
    KEY idx_provider_model (provider, model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='每次 AI 调用的使用记录，不保存 prompt/response 内容';
