from pydantic import BaseModel
from typing import List, Optional
from enum import Enum


class EnergyDemandType(str, Enum):
    HISTORICAL = "historical"
    FORECAST = "forecast"


class EnergyDemand(BaseModel):
    year: int
    value: float
    type: EnergyDemandType

    class Config:
        orm_mode = True


class FloodEvent(BaseModel):
    year: int
    events: int

    class Config:
        orm_mode = True


class Mine(BaseModel):
    reference: str
    name: str
    status: str
    easting: float
    northing: float
    local_authority: Optional[str]
    note: Optional[str]
    flood_risk_level: Optional[str]
    flood_history: Optional[List[FloodEvent]] = []
    energy_demand: Optional[List[EnergyDemand]] = []

    class Config:
        orm_mode = True