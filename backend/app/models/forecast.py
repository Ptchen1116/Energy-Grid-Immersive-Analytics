from app.database import Base
from sqlalchemy import Column, Integer, String, Float
from app.database import Base
from sqlalchemy import UniqueConstraint



class RegionEnergyConsumption(Base):
    __tablename__ = "region_energy_consumption"
    id = Column(Integer, primary_key=True, index=True)
    region = Column(String(10), index=True, nullable=False)
    year = Column(Integer, index=True, nullable=False)
    consumption = Column(Float, nullable=False)
    source = Column(String(20), nullable=False)  # "actual" 或 "forecast"

    __table_args__ = (UniqueConstraint("region", "year", name="u_region_year"),)