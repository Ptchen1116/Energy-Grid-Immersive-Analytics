import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.user_pin import UserPin
from app.models.mine_site import Mine
from app.routes import user_pin as user_pin_routes
from typing import List

app.include_router(user_pin_routes.router)

class DummyQuery:
    def __init__(self, results: List):
        self.results = results

    def filter_by(self, **kwargs):
        filtered = self.results
        for key, value in kwargs.items():
            filtered = [r for r in filtered if getattr(r, key, None) == value]
        return DummyQuery(filtered)

    def filter(self, *args, **kwargs):
        filtered = self.results
        for expr in args:
            if hasattr(expr, "left") and hasattr(expr.left, "name") and hasattr(expr.right, "value"):
                key = expr.left.name
                val = expr.right.value
                filtered = [r for r in filtered if getattr(r, key, None) == val]
        return DummyQuery(filtered)

    def first(self):
        return self.results[0] if self.results else None

    def all(self):
        return self.results.copy()


class DummySession:
    def __init__(self):
        self.mines: List[Mine] = []
        self.user_pins: List[UserPin] = []
        self._next_id = 1

    def query(self, model):
        if model.__name__ == "Mine":
            return DummyQuery(self.mines)
        elif model.__name__ == "UserPin":
            return DummyQuery(self.user_pins)
        return DummyQuery([])

    def add(self, obj):
        if getattr(obj, "id", None) is None:
            obj.id = self._next_id
            self._next_id += 1
        if isinstance(obj, Mine):
            self.mines.append(obj)
        elif isinstance(obj, UserPin):
            self.user_pins.append(obj)

    def commit(self):
        pass

    def flush(self):
        pass

    def refresh(self, obj):
        container = self.mines if isinstance(obj, Mine) else self.user_pins
        for i, existing in enumerate(container):
            if getattr(existing, "id", None) == getattr(obj, "id", None):
                container[i] = obj

    def delete(self, obj):
        container = self.mines if isinstance(obj, Mine) else self.user_pins
        container[:] = [o for o in container if o != obj]


dummy_db = DummySession()
app.dependency_overrides[user_pin_routes.get_db] = lambda: dummy_db
client = TestClient(app)


@pytest.fixture
def setup_user_pins():
    dummy_db.mines.clear()
    dummy_db.user_pins.clear()
    dummy_db._next_id = 1
    pin = UserPin(user_id=1, mine_id=1, note="Initial note")
    dummy_db.add(pin)
    return dummy_db


def test_create_new_pin(setup_user_pins):
    response = client.post("/users/2/pins", json={"mine_id": 2, "note": "New note"})
    assert response.status_code == 200
    data = response.json()
    assert data["mine_id"] == 2
    assert data["note"] == "New note"
    assert "id" in data

def test_update_existing_pin(setup_user_pins):
    response = client.post("/users/1/pins", json={"mine_id": 1, "note": "Updated note"})
    assert response.status_code == 200
    data = response.json()
    assert data["note"] == "Updated note"
    assert data["id"] == 1

def test_read_pins(setup_user_pins):
    response = client.get("/users/1/pins")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) == 1
    assert data[0]["note"] == "Initial note"

def test_read_pin_by_mine_existing(setup_user_pins):
    response = client.get("/users/1/pins/mine/1")
    assert response.status_code == 200
    data = response.json()
    assert data["mine_id"] == 1
    assert data["note"] == "Initial note"

def test_read_pin_by_mine_nonexistent(setup_user_pins):
    response = client.get("/users/1/pins/mine/999")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == -1
    assert data["note"] is None

def test_delete_existing_pin(setup_user_pins):
    response = client.delete("/users/1/pins/mine/1")
    assert response.status_code == 200
    data = response.json()
    assert data["mine_id"] == 1

def test_delete_nonexistent_pin(setup_user_pins):
    response = client.delete("/users/1/pins/mine/999")
    assert response.status_code == 404