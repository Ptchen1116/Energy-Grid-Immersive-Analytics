from fastapi import FastAPI
from app.routes import forecast, user  
from app.database import Base, engine  
app = FastAPI()

app.include_router(forecast.router)
app.include_router(user.router, prefix="/users", tags=["users"])

Base.metadata.create_all(bind=engine)