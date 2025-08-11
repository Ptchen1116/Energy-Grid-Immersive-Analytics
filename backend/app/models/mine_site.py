from sqlalchemy import Column, String, Float, Integer, ForeignKey, Enum
from sqlalchemy.orm import relationship
from app.database import Base
import enum
from pydantic import BaseModel, ConfigDict
from typing import List, Optional

class EnergyDemandType(enum.Enum):
    HISTORICAL = "historical"
    FORECAST = "forecast"

class EnergyDemand(BaseModel):
    year: int
    value: float

class MineResponse(BaseModel):
    reference: str
    name: str
    status: Optional[str] = None
    easting: float
    northing: float
    localAuthority: Optional[str] = None
    floodRiskLevel: Optional[str] = None
    floodHistory: Optional[List] = []
    energyDemandHistory: Optional[List[EnergyDemand]] = []
    forecastEnergyDemand: Optional[List[EnergyDemand]] = []
    trend: Optional[str] = None

    model_config = ConfigDict(
        alias_generator=lambda string: ''.join(
            word.capitalize() if i > 0 else word
            for i, word in enumerate(string.split('_'))
        ),
        populate_by_name=True  
    )


class Mine(Base):
    __tablename__ = "mines"

    reference = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    status = Column(String, nullable=False)  # "C" = closed, "I" = closing
    easting = Column(Float, nullable=False)
    northing = Column(Float, nullable=False)
    local_authority = Column(String, nullable=True)
    note = Column(String, nullable=True)
    flood_risk_level = Column(String, nullable=True)

    flood_history = relationship("FloodEvent", back_populates="mine", cascade="all, delete")
    energy_demand = relationship("EnergyDemand", back_populates="mine", cascade="all, delete")
    


class FloodEvent(Base):
    __tablename__ = "flood_events"

    id = Column(Integer, primary_key=True, index=True)
    year = Column(Integer, nullable=False)
    events = Column(Integer, nullable=False)
    mine_reference = Column(String, ForeignKey("mines.reference"), nullable=False)
    mine = relationship("Mine", back_populates="flood_history")


class EnergyDemand(Base):
    __tablename__ = "energy_demand"

    id = Column(Integer, primary_key=True, index=True)
    year = Column(Integer, nullable=False)
    value = Column(Float, nullable=False)
    type = Column(Enum(EnergyDemandType), nullable=False)  # historical / forecast
    mine_reference = Column(String, ForeignKey("mines.reference"), nullable=False)
    mine = relationship("Mine", back_populates="energy_demand")