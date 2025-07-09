from pydantic import BaseModel
from typing import Optional

class UserPinCreate(BaseModel):
    mine_id: int
    note: Optional[str] = None

class UserPinResponse(UserPinCreate):
    id: int
    class Config:
        orm_mode = True