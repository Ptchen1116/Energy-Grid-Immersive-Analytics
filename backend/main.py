from fastapi import FastAPI
from app.routes import forecast, user,  user_pin, energy_site
from app.database import Base, engine  
app = FastAPI()

app.include_router(forecast.router)
app.include_router(user.router, prefix="/users", tags=["users"])
app.include_router(user.router, prefix="/users", tags=["users"])
app.include_router(user_pin.router, prefix="/api", tags=["pins"])
app.include_router(energy_site.router)

Base.metadata.create_all(bind=engine)
