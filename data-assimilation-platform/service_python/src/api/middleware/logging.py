import logging
import time
from fastapi import Request


def setup_logging(app):
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    @app.middleware("http")
    async def log_requests(request: Request, call_next):
        start = time.perf_counter()
        response = await call_next(request)
        elapsed = time.perf_counter() - start
        logging.info(
            f"{request.method} {request.url.path} "
            f"- {response.status_code} - {elapsed:.3f}s"
        )
        return response
