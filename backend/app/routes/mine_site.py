from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.mine_site import Mine, EnergyDemand, EnergyDemandType,MineResponse
from app.schemas.mine_site import Mine as MineSchema
import json
from pathlib import Path
from app.utils import get_current_user_optional
from app.models.user import User
from app.models.user_pin import UserPin
from typing import Optional


router = APIRouter()

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent  
ASSETS_DIR = PROJECT_ROOT / "app/src/main/assets"
JSON_PATH = ASSETS_DIR / "fake_mine_location_data.json"

def load_mines_from_json(db: Session, json_file: Path, target_ref: str = None):
    with open(json_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    mines_loaded = []
    for obj in data:
        if target_ref and obj["Reference"] != target_ref:
            continue

        mine_ref = obj["Reference"]
        mine = db.query(Mine).filter_by(reference=mine_ref).first()
        if not mine:
            mine = Mine(
                reference=mine_ref,
                name=obj["Name"],
                status=obj["Status"],
                easting=float(obj["Easting"]),
                northing=float(obj["Northing"]),
                local_authority=obj.get("LocalAuthority"),
                note=obj.get("Note"),
                flood_risk_level=obj.get("FloodRiskLevel"),
            )
            db.add(mine)
            db.flush()

        energy_history = []  

        if obj.get("EnergyDemandHistory"):
            for ed in obj["EnergyDemandHistory"]:
                year = ed.get("year")
                value = ed.get("value")
                if year is not None and value is not None:
                    exists = db.query(EnergyDemand).filter_by(
                        mine_reference=mine_ref,
                        year=year,
                        type=EnergyDemandType.HISTORICAL
                    ).first()
                    if not exists:
                        demand = EnergyDemand(
                            year=year,
                            value=value,
                            type=EnergyDemandType.HISTORICAL,
                            mine_reference=mine_ref
                        )
                        db.add(demand)
                    energy_history.append(EnergyDemand(year=year, value=value, type=EnergyDemandType.HISTORICAL))

        if obj.get("ForecastEnergyDemand"):
            for ed in obj["ForecastEnergyDemand"]:
                year = ed.get("year")
                value = ed.get("value")
                if year is not None and value is not None:
                    exists = db.query(EnergyDemand).filter_by(
                        mine_reference=mine_ref,
                        year=year,
                        type=EnergyDemandType.FORECAST
                    ).first()
                    if not exists:
                        demand = EnergyDemand(
                            year=year,
                            value=value,
                            type=EnergyDemandType.FORECAST,
                            mine_reference=mine_ref
                        )
                        db.add(demand)

        energy_history_sorted = sorted(energy_history, key=lambda x: x.year)

        mines_loaded.append(mine)

    db.commit()
    return mines_loaded


def calculate_trend(energy_history: list[EnergyDemand]) -> str | None:
    if len(energy_history) < 2:
        return None
    first = energy_history[0].value
    last = energy_history[-1].value
    if last > first:
        return "INCREASING"
    elif last < first:
        return "DECREASING"
    else:
        return "STABLE"

@router.get("/mines/{reference}", response_model=MineResponse)
def get_mine(
    reference: str, 
    db: Session = Depends(get_db), 
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    mine = db.query(Mine).filter(Mine.reference == reference).first()
    if not mine:
        raise HTTPException(status_code=404, detail="Mine not found")

    user_note = None
    if current_user:
        user_pin = db.query(UserPin).filter_by(user_id=current_user.id, mine_id=mine.id).first()
        user_note = user_pin.note if user_pin else None

    historical = [e for e in mine.energy_demand if e.type == EnergyDemandType.HISTORICAL]
    trend = calculate_trend(historical)

    return {
        "reference": mine.reference,
        "name": mine.name,
        "status": mine.status,
        "easting": mine.easting,
        "northing": mine.northing,
        "localAuthority": mine.local_authority,
        "note": user_note,  
        "floodRiskLevel": mine.flood_risk_level,
        "floodHistory": [{"year": f.year, "events": f.events} for f in mine.flood_history],
        "energyDemandHistory": [
            {"year": e.year, "value": e.value}
            for e in historical
        ],
        "forecastEnergyDemand": [
            {"year": e.year, "value": e.value}
            for e in mine.energy_demand if e.type == EnergyDemandType.FORECAST
        ],
        "trend": trend
    }

@router.get("/mines", response_model=list[MineResponse])
def list_mines(db: Session = Depends(get_db)):
    mines = db.query(Mine).all()
    if not mines:
        mines = load_mines_from_json(db, JSON_PATH)
    return [orm_to_dict(mine) for mine in mines]


def to_camel(snake_str: str) -> str:
    parts = snake_str.split('_')
    return parts[0] + ''.join(word.capitalize() for word in parts[1:])

def orm_to_dict(mine: Mine) -> dict:
    historical = [e for e in mine.energy_demand if e.type == EnergyDemandType.HISTORICAL]
    trend = calculate_trend(sorted(historical, key=lambda x: x.year))

    data = {
        "reference": mine.reference,
        "name": mine.name,
        "status": mine.status,
        "easting": mine.easting,
        "northing": mine.northing,
        "localAuthority": mine.local_authority,
        "note": mine.note,
        "floodRiskLevel": mine.flood_risk_level,
        "floodHistory": [{"year": f.year, "events": f.events} for f in mine.flood_history],
        "energyDemandHistory": [
            {"year": e.year, "value": e.value}
            for e in historical
        ],
        "forecastEnergyDemand": [
            {"year": e.year, "value": e.value}
            for e in mine.energy_demand if e.type == EnergyDemandType.FORECAST
        ],
        "trend": trend,
    }
    return data