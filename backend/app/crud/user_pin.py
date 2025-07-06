# backend/app/crud/user_pin.py
from sqlalchemy.orm import Session
from app.models.user_pin import UserPin
from app.schemas.user_pin import UserPinCreate

def create_user_pin(db: Session, user_id: int, pin_data: UserPinCreate):
    pin = UserPin(user_id=user_id, mine_id=pin_data.mine_id, note=pin_data.note)
    db.add(pin)
    db.commit()
    db.refresh(pin)
    return pin

def get_user_pins(db: Session, user_id: int):
    return db.query(UserPin).filter(UserPin.user_id == user_id).all()

def get_user_pin_by_mine(db: Session, user_id: int, mine_id: int):
    return db.query(UserPin).filter(
        UserPin.user_id == user_id,
        UserPin.mine_id == mine_id
    ).first()