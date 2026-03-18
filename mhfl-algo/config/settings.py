from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    # Redis 配置
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = "12345678"
    REDIS_DB: int = 0
    REDIS_MAX_CONNECTIONS: int = 100
    REDIS_SOCKET_TIMEOUT: int = 5

    # 智能助手配置（.env 中配置，OPENAI_API_KEY 必填）
    OPENAI_API_KEY: str = ""
    OPENAI_API_BASE: str | None = None
    ASSISTANT_MODEL: str = "gpt-4o-mini"
    ASSISTANT_EMBEDDING_MODEL: str = "text-embedding-3-small"
    ASSISTANT_KB_DIR: str = "assistant/knowledge/raw"
    ASSISTANT_CHROMA_DIR: str = "assistant/knowledge/chroma_db"
    ASSISTANT_TOP_K: int = 8
    ASSISTANT_RERANK_TOP_N: int = 4
    ASSISTANT_CHUNK_SIZE: int = 400
    ASSISTANT_CHUNK_OVERLAP: int = 50
    ASSISTANT_ENABLE_RERANK: bool = False
    ASSISTANT_ENABLE_MULTI_QUERY: bool = False

    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()
