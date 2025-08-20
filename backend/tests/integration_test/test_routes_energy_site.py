import os
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from app.main import app
from app.routes import energy_site as site_router
from app.database import Base, get_db
from app.models.energy_site import EnergySite

# --- 測試用 in-memory SQLite ---
SQLALCHEMY_TEST_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# --- 建立表格 ---
Base.metadata.create_all(bind=engine)

# --- 覆寫 get_db ---
def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db
app.include_router(site_router.router)

client = TestClient(app)

# --- Fixture: 清空表格 ---
@pytest.fixture(autouse=True)
def clean_db():
    db = TestingSessionLocal()
    db.query(EnergySite).delete()
    db.commit()
    db.close()
    yield

# --- Integration Tests ---
def test_get_solar_sites_creates_entries():
    response = client.get("/sites/solar")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # 回傳至少一個站點
    assert len(data) > 0
    # 確認資料庫已新增
    db = TestingSessionLocal()
    count = db.query(EnergySite).filter(EnergySite.type == "solar").count()
    assert count == len(data)
    db.close()

def test_get_unknown_category_returns_empty():
    response = client.get("/sites/unknown")
    assert response.status_code == 200
    assert response.json() == []

def test_get_wind_sites_creates_entries():
    response = client.get("/sites/wind")
    assert response.status_code == 200
    data = response.json()
    assert all(site["name"] for site in data)