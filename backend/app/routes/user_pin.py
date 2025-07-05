# backend/app/routes/user_pin.py
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.schemas.user_pin import UserPinCreate, UserPinResponse
from app.crud import user_pin as crud_user_pin

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/users/{user_id}/pins", response_model=UserPinResponse)
def create_pin(user_id: int, pin_data: UserPinCreate, db: Session = Depends(get_db)):
    return crud_user_pin.create_user_pin(db, user_id, pin_data)

@router.get("/users/{user_id}/pins", response_model=list[UserPinResponse])
def read_pins(user_id: int, db: Session = Depends(get_db)):
    return crud_user_pin.get_user_pins(db, user_id)