import os
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from unittest.mock import patch  # ✅ 忘了 import

from app.main import app
from app.database import Base, get_db
from app.models.user import User  # 你的 User SQLAlchemy model

# --- 1️⃣ 設定測試環境變數 ---
os.environ["TESTING"] = "1"
os.environ["SECRET_KEY"] = "testsecret"
os.environ["ALGORITHM"] = "HS256"
os.environ["ACCESS_TOKEN_EXPIRE_MINUTES"] = "30"

# --- 2️⃣ 使用 in-memory SQLite ---
SQLALCHEMY_TEST_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# --- 3️⃣ 建立資料表 ---
Base.metadata.create_all(bind=engine)

# --- 4️⃣ 覆寫 get_db ---
def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

# --- Fixture 建立測試 client ---
@pytest.fixture
def client():
    return TestClient(app)

# --- 1️⃣ Register 測試 ---
def test_register_user(client):
    payload = {
        "username": "testuser",
        "email": "test@example.com",
        "password": "password123"
    }
    response = client.post("/users/register", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == payload["email"]
    assert data["username"] == payload["username"]
    assert "id" in data

# --- 2️⃣ Login 測試 (mock authenticate_user) ---
class FakeUser:
    def __init__(self, id):
        self.id = id

def test_login_user(client):
    payload = {
        "email": "test@example.com",
        "password": "password123"
    }
    with patch("app.routes.user.authenticate_user") as mock_auth:
        mock_auth.return_value = FakeUser(id=1)

        response = client.post("/users/login", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["token_type"] == "bearer"

# --- 3️⃣ Login 失敗測試 ---
def test_login_fail(client):
    payload = {"email": "wrong@example.com", "password": "wrong"}
    with patch("app.routes.user.authenticate_user") as mock_auth:
        mock_auth.return_value = None  # 模擬登入失敗

        response = client.post("/users/login", json=payload)
        assert response.status_code == 401
        assert response.json() == {"detail": "Invalid email or password"}