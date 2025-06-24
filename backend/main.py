from fastapi import FastAPI
from app.routes import forecast

app = FastAPI()

app.include_router(forecast.router)