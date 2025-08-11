from pydantic import BaseModel, ConfigDict
from typing import Optional

class UserPinCreate(BaseModel):
    mine_id: int
    note: Optional[str] = None

class UserPinResponse(UserPinCreate):
    id: int
    model_config = ConfigDict(from_attributes=True)