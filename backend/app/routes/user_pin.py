# backend/app/routes/user_pin.py
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import SessionLocal, get_db
from app.crud import user_pin as crud_user_pin
from fastapi import HTTPException, status
from app.schemas.user_pin import UserPinCreate, UserPinResponse
from app.models.user_pin import UserPin



router = APIRouter()

@router.post("/users/{user_id}/pins", response_model=UserPinResponse)
def create_or_update_pin(user_id: int, pin_data: UserPinCreate, db: Session = Depends(get_db)):
    existing_pin = crud_user_pin.get_user_pin_by_mine(db, user_id, pin_data.mine_id)
    if existing_pin:
        existing_pin.note = pin_data.note
        db.commit()
        db.refresh(existing_pin)
        return existing_pin
    else:
        new_pin = UserPin(user_id=user_id, mine_id=pin_data.mine_id, note=pin_data.note)
        db.add(new_pin)
        db.commit()
        db.refresh(new_pin)
        return new_pin

@router.get("/users/{user_id}/pins", response_model=list[UserPinResponse])
def read_pins(user_id: int, db: Session = Depends(get_db)):
    return crud_user_pin.get_user_pins(db, user_id)

@router.get("/users/{user_id}/pins/mine/{mine_id}", response_model=UserPinResponse)
def read_pin_by_mine(user_id: int, mine_id: int, db: Session = Depends(get_db)):
    pin = crud_user_pin.get_user_pin_by_mine(db, user_id, mine_id)
    if not pin:
        return UserPinResponse(id=-1,mine_id=mine_id, note=None)  
    return pin

@router.delete("/users/{user_id}/pins/mine/{mine_id}", response_model=UserPinResponse)
def delete_pin(user_id: int, mine_id: int, db: Session = Depends(get_db)):
    pin = crud_user_pin.get_user_pin_by_mine(db, user_id, mine_id)
    if not pin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Pin for user {user_id} and mine {mine_id} not found"
        )
    
    db.delete(pin)
    db.commit()
    return pin