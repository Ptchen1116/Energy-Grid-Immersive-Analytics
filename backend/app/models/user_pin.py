from sqlalchemy import Column, Integer, Text, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base

class UserPin(Base):
    __tablename__ = "user_pins"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    mine_id = Column(Text) 
    note = Column(Text)

    user = relationship("User", back_populates="pins")
