import os
import json
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from app.main import app
from app.routes import mine_site as mines_router
from app.database import Base, get_db
from app.models.mine_site import Mine, FloodEvent, EnergyDemand, EnergyDemandType

# --- 1️⃣ 設定測試環境變數，避免 router 自動 load JSON ---
os.environ["TESTING"] = "1"

# --- 2️⃣ 使用 in-memory SQLite ---
SQLALCHEMY_TEST_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    "sqlite:///:memory:",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# --- 3️⃣ 建立表格 (必須在 router include 前) ---
Base.metadata.create_all(bind=engine)

# --- 4️⃣ 覆寫 get_db ---
def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

# --- 5️⃣ include router ---
app.include_router(mines_router.router)

# --- 6️⃣ 建立 TestClient ---
client = TestClient(app)

# --- 7️⃣ fixture: 插入 fake mine 資料 ---
@pytest.fixture
def setup_fake_mines():
    db = TestingSessionLocal()
    mine = Mine(
        reference="MINE1",
        name="Test Mine",
        status="Open",
        easting=123.0,
        northing=456.0,
        local_authority="Test Authority",
        note="Initial note",
        flood_risk_level="High",
    )
    db.add(mine)
    db.commit()
    db.refresh(mine)

    # Flood event
    flood = FloodEvent(year=2020, events=2, mine_reference=mine.reference)
    db.add(flood)

    # Energy demand history (2 points for STABLE trend)
    energy_hist1 = EnergyDemand(
        year=2020,
        value=100.0,
        type=EnergyDemandType.HISTORICAL,
        mine_reference=mine.reference,
    )
    energy_hist2 = EnergyDemand(
        year=2021,
        value=100.0,
        type=EnergyDemandType.HISTORICAL,
        mine_reference=mine.reference,
    )

    # Forecast
    energy_forecast = EnergyDemand(
        year=2025,
        value=150.0,
        type=EnergyDemandType.FORECAST,
        mine_reference=mine.reference,
    )

    db.add_all([energy_hist1, energy_hist2, energy_forecast])
    db.commit()

    yield

    # 清理資料
    db.query(EnergyDemand).delete()
    db.query(FloodEvent).delete()
    db.query(Mine).delete()
    db.commit()
    db.close()

# --- 8️⃣ 測試 ---
def test_list_mines(setup_fake_mines):
    response = client.get("/mines")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) == 1
    assert data[0]["reference"] == "MINE1"
    assert "energyDemandHistory" in data[0]
    assert "forecastEnergyDemand" in data[0]
    assert data[0]["trend"] == "STABLE"

def test_get_mine(setup_fake_mines):
    response = client.get("/mines/MINE1")
    assert response.status_code == 200
    mine = response.json()
    assert mine["reference"] == "MINE1"
    assert mine["floodHistory"][0]["year"] == 2020
    assert mine["energyDemandHistory"][0]["value"] == 100.0
    assert mine["forecastEnergyDemand"][0]["value"] == 150.0

def test_get_mine_not_found():
    response = client.get("/mines/UNKNOWN")
    assert response.status_code == 404

def test_debug_floods(setup_fake_mines):
    response = client.get("/debug/floods")
    assert response.status_code == 200
    data = response.json()
    assert data[0]["mine"] == "MINE1"
    assert data[0]["floodHistoryCount"] == 1
    assert data[0]["floodHistory"][0]["events"] == 2