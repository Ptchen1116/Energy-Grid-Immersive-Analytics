from pydantic import BaseModel

class EnergySiteCreate(BaseModel):
    name: str
    type: str
    latitude: float
    longitude: float

class EnergySiteRead(EnergySiteCreate):
    id: int

    class Config:
        orm_mode = True