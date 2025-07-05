from pydantic import BaseModel

class UserPinCreate(BaseModel):
    mine_id: int
    note: str

class UserPinResponse(UserPinCreate):
    id: int
    class Config:
        orm_mode = True