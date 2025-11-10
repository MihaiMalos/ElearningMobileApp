from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.user import User, UserRole
from app.utils.security import get_password_hash


class UserRepository:
    """Repository for User entity operations."""
    
    def __init__(self, db: Session):
        self.db = db
    
    def get_by_id(self, user_id: int) -> Optional[User]:
        """Get user by ID."""
        return self.db.query(User).filter(User.id == user_id).first()
    
    def get_by_username(self, username: str) -> Optional[User]:
        """Get user by username."""
        return self.db.query(User).filter(User.username == username).first()
    
    def get_by_email(self, email: str) -> Optional[User]:
        """Get user by email."""
        return self.db.query(User).filter(User.email == email).first()
    
    def get_all(self, skip: int = 0, limit: int = 100) -> List[User]:
        """Get all users with pagination."""
        return self.db.query(User).offset(skip).limit(limit).all()
    
    def get_by_role(self, role: UserRole, skip: int = 0, limit: int = 100) -> List[User]:
        """Get users by role with pagination."""
        return self.db.query(User).filter(User.role == role).offset(skip).limit(limit).all()
    
    def create(
        self,
        email: str,
        username: str,
        password: str,
        role: UserRole
    ) -> User:
        """Create a new user."""
        hashed_password = get_password_hash(password)
        db_user = User(
            email=email,
            username=username,
            hashed_password=hashed_password,
            role=role
        )
        self.db.add(db_user)
        self.db.commit()
        self.db.refresh(db_user)
        return db_user
    
    def update(
        self,
        user: User,
        email: Optional[str] = None,
        username: Optional[str] = None,
        password: Optional[str] = None
    ) -> User:
        """Update user information."""
        if email is not None:
            user.email = email
        if username is not None:
            user.username = username
        if password is not None:
            user.hashed_password = get_password_hash(password)
        
        self.db.commit()
        self.db.refresh(user)
        return user
    
    def delete(self, user: User) -> None:
        """Delete a user."""
        self.db.delete(user)
        self.db.commit()
