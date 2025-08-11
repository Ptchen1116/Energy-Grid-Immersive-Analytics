from fastapi.testclient import TestClient
from unittest.mock import patch
import pytest

from app.main import app
from app.schemas.user import UserLogin

class FakeUser:
    def __init__(self, id):
        self.id = id

@pytest.fixture
def client():
    return TestClient(app)

def test_login_success(client):
    with patch("app.routes.user.authenticate_user") as mock_auth:
        mock_auth.return_value = FakeUser(id=123)

        response = client.post("/users/login", json={"email": "test@example.com", "password": "password"})

        assert response.status_code == 200
        json_resp = response.json()
        assert "access_token" in json_resp
        assert json_resp["token_type"] == "bearer"

def test_login_fail(client):
    with patch("app.routes.user.authenticate_user") as mock_auth:
        mock_auth.return_value = None

        response = client.post("/users/login", json={"email": "wrong@example.com", "password": "wrong"})

        assert response.status_code == 401
        assert response.json() == {"detail": "Invalid email or password"}