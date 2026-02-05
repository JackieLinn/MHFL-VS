import redis
from config.settings import settings

pool = redis.ConnectionPool(
    host=settings.REDIS_HOST,
    port=settings.REDIS_PORT,
    password=settings.REDIS_PASSWORD or None,
    db=settings.REDIS_DB,
    decode_responses=True,
    socket_connect_timeout=settings.REDIS_SOCKET_TIMEOUT,
    max_connections=settings.REDIS_MAX_CONNECTIONS
)


def get_redis_client() -> redis.Redis:
    """获取 Redis 客户端实例"""
    return redis.Redis(connection_pool=pool)
