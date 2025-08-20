import pytest
from fastapi import HTTPException
from unittest.mock import MagicMock
from app.routes import user as user_module  # 導入你的 user route module
from app.schemas.user import UserLogin
from app.schemas.user import UserCreate, UserLogin

class FakeUser:
    def __init__(self, id, email=None, username=None):
        self.id = id
        self.email = email
        self.username = username

@pytest.fixture
def fake_db():
    return MagicMock()

def test_register_unit(fake_db):
    # 模擬 create_user 函數
    def fake_create_user(db, user_create: UserCreate):
        return {"id": 1, "email": user_create.email, "username": user_create.username}

    user_module.create_user = fake_create_user

    user_data = UserCreate(username="testuser", email="test@example.com", password="password123")
    response = user_module.register_user(user_data, db=fake_db)

    assert response["email"] == user_data.email
    assert response["username"] == user_data.username
    assert "id" in response

def test_login_success_unit(fake_db):
    user_module.authenticate_user = lambda db, email, password: FakeUser(id=123)

    user_login = UserLogin(email="test@example.com", password="password")
    response = user_module.login(user_login, db=fake_db)

    assert "access_token" in response
    assert response["token_type"] == "bearer"

def test_login_fail_unit(fake_db):
    user_module.authenticate_user = lambda db, email, password: None

    user_login = UserLogin(email="wrong@example.com", password="wrong")
    with pytest.raises(HTTPException) as excinfo:
        user_module.login(user_login, db=fake_db)

    assert excinfo.value.status_code == 401
    assert excinfo.value.detail == "Invalid email or password"