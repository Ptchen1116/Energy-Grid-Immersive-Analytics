import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.routes import user_pin as user_pin_routes
from app.database import Base, get_db

# --- 1️⃣ 使用 file-based SQLite 測試 DB ---
SQLALCHEMY_TEST_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(
    SQLALCHEMY_TEST_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# --- 2️⃣ 建立所有表 ---
Base.metadata.create_all(bind=engine)

# --- 3️⃣ 覆寫 get_db ---
def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

# --- 4️⃣ include router ---
app.include_router(user_pin_routes.router)

client = TestClient(app)

# --- 5️⃣ pytest fixture ---
@pytest.fixture
def setup_dummy_data():
    # 可在此建立測試初始資料
    # 例如新增 user 或 mine，如果需要的話
    yield
    # 測試結束後清空表
    db = TestingSessionLocal()
    db.query(Base.metadata.tables['user_pins']).delete()
    db.commit()
    db.close()

# --- 6️⃣ CRUD 流程測試 ---
def test_user_pin_crud_flow(setup_dummy_data):
    # Create
    response = client.post("/users/2/pins", json={"mine_id": 2, "note": "New note"})
    assert response.status_code == 200
    data = response.json()
    assert data.get("mine_id") == 2
    assert data.get("note") == "New note"
    assert "id" in data
    new_pin_id = data["id"]

    # Read all
    response = client.get("/users/2/pins")
    assert response.status_code == 200
    pins = response.json()
    assert isinstance(pins, list)
    assert any(p["id"] == new_pin_id for p in pins)

    # Read by mine_id
    response = client.get(f"/users/2/pins/mine/2")
    assert response.status_code == 200
    pin = response.json()
    assert pin["mine_id"] == 2
    assert pin["note"] == "New note"

    # Update
    response = client.post("/users/2/pins", json={"mine_id": 2, "note": "Updated note"})
    assert response.status_code == 200
    updated_pin = response.json()
    assert updated_pin["id"] == new_pin_id
    assert updated_pin["note"] == "Updated note"

    # Delete
    response = client.delete("/users/2/pins/mine/2")
    assert response.status_code == 200
    deleted_pin = response.json()
    assert deleted_pin["id"] == new_pin_id

    # Confirm deletion
    response = client.get("/users/2/pins/mine/2")
    assert response.status_code == 200
    pin = response.json()
    assert pin["id"] == -1
    assert pin["note"] is None