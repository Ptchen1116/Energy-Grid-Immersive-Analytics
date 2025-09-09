# tests/routes/test_mine_site.py
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.routes import mine_site  
from app.models.mine_site import Mine, EnergyDemand, EnergyDemandType
from app.models.user_pin import UserPin

class DummyQuery:
    def __init__(self, results):
        self.results = results

    def filter_by(self, **kwargs):
        ref = kwargs.get("reference") or kwargs.get("mine_reference")
        if ref:
            filtered = [m for m in self.results if m.reference == ref]
            return DummyQuery(filtered)
        return DummyQuery(self.results)

    def filter(self, *args, **kwargs):
        filtered = []
        for item in self.results:
            for expr in args:
                if hasattr(expr, 'left') and hasattr(expr.left, 'name') and expr.left.name == 'reference':
                    if getattr(expr.right, 'value', None) == item.reference:
                        filtered.append(item)
        return DummyQuery(filtered)

    def first(self):
        return self.results[0] if self.results else None

    def all(self):
        return self.results

class DummySession:
    def __init__(self):
        self.mines = []
        self.user_pins = []

    def query(self, model):
        if model.__name__ == "Mine":
            return DummyQuery(self.mines)
        elif model.__name__ == "UserPin":
            return DummyQuery(self.user_pins)
        return DummyQuery([])

    def add(self, obj):
        if isinstance(obj, Mine):
            self.mines.append(obj)
        elif isinstance(obj, UserPin):
            self.user_pins.append(obj)

    def commit(self):
        pass

    def flush(self):
        pass

client = TestClient(app)

dummy_db = DummySession()
app.dependency_overrides[mine_site.get_db] = lambda: dummy_db

@pytest.fixture
def setup_mines():
    dummy_db.mines.clear()
    dummy_db.user_pins.clear()

    mine1 = Mine(
        reference="MINE1",
        name="Test Mine 1",
        status="Active",
        easting=100,
        northing=200,
        local_authority="Authority1",
        note="Original Note",
        flood_risk_level="High",
        energy_demand=[
            EnergyDemand(year=2020, value=100, type=EnergyDemandType.HISTORICAL),
            EnergyDemand(year=2021, value=120, type=EnergyDemandType.HISTORICAL),
            EnergyDemand(year=2022, value=150, type=EnergyDemandType.FORECAST)
        ],
        flood_history=[]
    )
    dummy_db.add(mine1)
    dummy_db.add(UserPin(user_id=1, mine_id=1, note="User note"))
    return dummy_db

def test_get_mine_route(setup_mines):
    response = client.get("/mines/MINE1")
    assert response.status_code == 200
    data = response.json()
    assert data["reference"] == "MINE1"
    assert data["trend"] == "INCREASING"  
    assert "energyDemandHistory" in data
    assert data.get("note") is None   

def test_list_mines_route(setup_mines):
    response = client.get("/mines")
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1  
    assert data[0]["reference"] == "MINE1"