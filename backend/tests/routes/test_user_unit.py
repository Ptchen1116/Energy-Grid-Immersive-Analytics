import pytest
from fastapi import HTTPException
from unittest.mock import MagicMock
from app.routes import user as user_module  # 導入你的 user route module
from app.schemas.user import UserLogin

class FakeUser:
    def __init__(self, id):
        self.id = id

@pytest.fixture
def fake_db():
    return MagicMock()

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