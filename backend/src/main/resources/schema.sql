CREATE TABLE future_plan (
    plan_id VARCHAR(36) NOT NULL,            -- 一意の識別子 (UUIDを格納)
    account_id VARCHAR(36) NOT NULL,         -- 申請したユーザーのアカウントID
    plan_date DATE NOT NULL,                 -- 予定の対象日 (yyyy-MM-dd)
    plan_title VARCHAR(100) NOT NULL,        -- 予定の区分・タイトル (自由入力テキスト)
    plan_detail VARCHAR(500),                -- 理由や備考・詳細 (空欄も許容)
    plan_status INT NOT NULL DEFAULT 0,      -- 承認状態 (0:申請中, 1:差戻し, 2:承認済み)
    admin_comment VARCHAR(500),              -- 管理者からの差戻理由コメント
    created_at TIMESTAMP NOT NULL,           -- 申請日時
    PRIMARY KEY (plan_id)
);