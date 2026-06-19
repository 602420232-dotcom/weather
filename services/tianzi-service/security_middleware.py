"""
Security Middleware for TianZi Service

Provides JWT authentication and security utilities.
"""

import logging
import os
from typing import Optional

logger = logging.getLogger(__name__)


class SecurityMiddleware:
    """
    Security middleware for JWT token verification.
    """

    def __init__(self, secret_key: Optional[str] = None):
        self.secret_key = (
            secret_key
            or os.getenv("JWT_SECRET", "")
            or os.getenv("JWT_SECRET_KEY", "")
        )
        if not self.secret_key:
            logger.warning(
                "JWT_SECRET not configured — authentication DISABLED. "
                "Set JWT_SECRET or JWT_SECRET_KEY environment variable."
            )

    async def verify(self, request):
        """
        Verify JWT token from request.
        Args:
            request: FastAPI request object
        Raises:
            HTTPException: If token is invalid or missing
        """
        if not self.secret_key:
            return

        from fastapi import HTTPException, status
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "AUTH_001", "message": "Missing or invalid Authorization header"},
                headers={"WWW-Authenticate": "Bearer"},
            )

        token = auth_header.split(" ")[1]
        try:
            import jwt
            from jwt import ExpiredSignatureError, InvalidSignatureError, DecodeError
            jwt.decode(
                token,
                self.secret_key,
                algorithms=["HS512"],
                options={"verify_aud": False}
            )
        except ExpiredSignatureError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "AUTH_003", "message": "Token has expired"},
                headers={"WWW-Authenticate": "Bearer"},
            )
        except InvalidSignatureError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "AUTH_004", "message": "Invalid token signature"},
                headers={"WWW-Authenticate": "Bearer"},
            )
        except DecodeError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "AUTH_002", "message": "Invalid token format"},
                headers={"WWW-Authenticate": "Bearer"},
            )
        except Exception as e:
            logger.error(f"JWT verification failed: {e}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={"code": "AUTH_ERROR", "message": "Authentication failed"},
                headers={"WWW-Authenticate": "Bearer"},
            )

    def extract_user_info(self, token: str):
        """
        Extract user information from JWT token.
        Args:
            token: JWT token string
        Returns:
            dict: User claims
        """
        if not self.secret_key:
            return {}

        try:
            import jwt
            return jwt.decode(
                token,
                self.secret_key,
                algorithms=["HS512"],
                options={"verify_aud": False}
            )
        except Exception as e:
            logger.error(f"Failed to extract user info: {e}")
            return {}
