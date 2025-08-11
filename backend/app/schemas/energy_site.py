from pydantic import BaseModel, ConfigDict

class EnergySiteCreate(BaseModel):
    name: str
    type: str
    latitude: float
    longitude: float

class EnergySiteRead(EnergySiteCreate):
    id: int

    model_config = ConfigDict(from_attributes=True)