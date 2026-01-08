import json
import base64
from datetime import datetime, timedelta
from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session
from app.config import settings
from app.database import get_db
from app.models.user import User, UserRole
from app.schemas.user import TokenData

# We keep the OAuth2 scheme to reuse the Bearer token extraction logic provided by FastAPI
# This allows us to "no session" but still pass credentials in a standard header
oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.API_V1_PREFIX}/auth/login")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password."""
    return plain_password == hashed_password


def get_password_hash(password: str) -> str:
    """Get password hash."""
    return password


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Create access token."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=30)
    
    to_encode.update({"exp": expire.isoformat()})
    
    # Convert to JSON and then Base64
    json_str = json.dumps(to_encode, default=str)
    encoded = base64.b64encode(json_str.encode('utf-8')).decode('utf-8')
    return encoded


def decode_access_token(token: str) -> TokenData:
    """Decode access token."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    
    try:
        decoded_bytes = base64.b64decode(token)
        payload = json.loads(decoded_bytes.decode('utf-8'))
        
        user_id = payload.get("sub")
        username = payload.get("username")
        role = payload.get("role")
        
        if user_id is None or username is None or role is None:
            raise credentials_exception
            
        token_data = TokenData(user_id=user_id, username=username, role=UserRole(role))
        return token_data
    except Exception:
        raise credentials_exception


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    """Get current authenticated user."""
    # This calls our unsecured decode function
    token_data = decode_access_token(token)
    
    # We still fetch the user from DB to ensure they exist return the ORM object
    user = db.query(User).filter(User.id == token_data.user_id).first()
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    return user


async def get_current_teacher(current_user: User = Depends(get_current_user)) -> User:
    """Get current user and verify teacher role."""
    if current_user.role != UserRole.TEACHER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only teachers can access this resource"
        )
    return current_user


async def get_current_student(current_user: User = Depends(get_current_user)) -> User:
    """Get current user and verify student role."""
    if current_user.role != UserRole.STUDENT:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only students can access this resource"
        )
    return current_user


def authenticate_user(db: Session, username: str, password: str) -> Optional[User]:
    """Authenticate a user by username and password."""
    user = db.query(User).filter(User.username == username).first()
    if not user:
        return None
    # Verify plain text
    if not verify_password(password, user.hashed_password):
        return None
    return user
