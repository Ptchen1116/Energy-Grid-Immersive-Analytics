from app.database import Base
from sqlalchemy import Column, Integer, String, Float
from sqlalchemy.orm import relationship  
from app.database import Base


class EnergySite(Base):
    __tablename__ = "energy_sites"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    type = Column(String, nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)