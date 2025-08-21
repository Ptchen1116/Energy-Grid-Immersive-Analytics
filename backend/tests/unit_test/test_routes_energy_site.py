import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.database import get_db
from app.models.energy_site import EnergySite

client = TestClient(app)

# -------------------------------
# Dummy DB session for testing
# -------------------------------
class DummySession:
    def __init__(self):
        self.added = []
        self.committed = False

    def query(self, model):
        return self

    def filter(self, *args, **kwargs):
        return self

    def all(self):
        return []

    def add(self, obj):
        self.added.append(obj)

    def commit(self):
        self.committed = True

# Fixture for mock DB
@pytest.fixture
def mock_db():
    return DummySession()

# -------------------------------
# Test for invalid category
# -------------------------------
def test_get_sites_invalid_category(mock_db):
    app.dependency_overrides[get_db] = lambda: mock_db
    response = client.get("/sites/unknown")
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json() == []

# -------------------------------
# Test for CSV import / solar sites
# -------------------------------
def test_get_sites_csv(mock_db, monkeypatch):
    mock_csv_data = [
        {
            "Technology Type": "Solar Photovoltaics",
            "Development Status (short)": "Operational",
            "Site Name": "Test Solar Site",
            "X-coordinate": "100000",
            "Y-coordinate": "200000"
        },
        {
            "Technology Type": "Wind Offshore",
            "Development Status (short)": "Operational",
            "Site Name": "Test Wind Site",
            "X-coordinate": "150000",
            "Y-coordinate": "250000"
        },
    ]

    class DummyDictReader:
        def __init__(self, f, *args, **kwargs):
            self.rows = mock_csv_data
        def __iter__(self):
            return iter(self.rows)

    # Override built-in open & csv.DictReader
    import builtins, csv
    monkeypatch.setattr(builtins, "open", lambda *args, **kwargs: None)
    monkeypatch.setattr(csv, "DictReader", DummyDictReader)

    # Override DB dependency
    app.dependency_overrides[get_db] = lambda: mock_db

    response = client.get("/sites/solar")
    app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    assert data[0]["name"] == "Test Solar Site"
    assert mock_db.committed is True
    assert isinstance(mock_db.added[0], EnergySite)