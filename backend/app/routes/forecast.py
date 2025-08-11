from fastapi import FastAPI
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Dict
import pandas as pd
from prophet import Prophet
import os
from fastapi import Depends
from sqlalchemy.future import select
from app.database import get_db
from app.models.forecast import RegionEnergyConsumption 
from sqlalchemy.orm import Session
from fastapi import HTTPException


app = FastAPI()
router = APIRouter()

BASE_DIR = os.path.dirname(os.path.abspath(__file__))  
data_dir = os.path.normpath(os.path.join(BASE_DIR, "..", "..", "..", "app", "src", "main", "assets", "Subnational_electricity_consumption_statistics"))

all_years = range(2005, 2024)
code_map = {
    "E12000001": "UKC",
    "E12000002": "UKD",
    "E12000003": "UKE",
    "E12000004": "UKF",
    "E12000005": "UKG",
    "E12000006": "UKH",
    "E12000007": "UKI",
    "E12000008": "UKJ",
    "E12000009": "UKK",
    "W92000004": "UKL",
    "S92000003": "UKM"
}
target_regions = list(code_map.values())

class ForecastRequest(BaseModel):
    year: int

all_data = []
for year in all_years:
    file_path = os.path.join(data_dir, f"Subnational_electricity_consumption_statistics_{year}.csv")
    df = pd.read_csv(file_path)
    df["year"] = year
    df["consumption"] = df["Total consumption\n(GWh):\nAll meters"]
    df["region"] = df["Code"].replace(code_map)
    df = df[df["region"].isin(target_regions)]
    all_data.append(df[["region", "year", "consumption"]])
df_all = pd.concat(all_data, ignore_index=True)

class ForecastRequest(BaseModel):
    year: int

@router.post("/forecast")
def forecast_energy(
    req: ForecastRequest,
    db: Session = Depends(get_db)  
) -> Dict[str, Dict[str, float | str]]:

    result = {}

    for region in target_regions:
        region_df = df_all[df_all["region"] == region][["year", "consumption"]].copy()

        if req.year in region_df["year"].values:
            actual_val = region_df[region_df["year"] == req.year]["consumption"].mean()
            result[region] = {
                "value": round(actual_val, 2),
                "source": "historical"
            }
            existing = db.query(RegionEnergyConsumption).filter_by(region=region, year=req.year).first()
            if not existing:
                db_record = RegionEnergyConsumption(
                    region=region,
                    year=req.year,
                     consumption=float(actual_val),
                    source="historical"
                )
                db.add(db_record)
            continue

        region_df["ds"] = pd.to_datetime(region_df["year"], format="%Y")
        region_df["y"] = region_df["consumption"]

        model = Prophet(yearly_seasonality=False)
        model.fit(region_df[["ds", "y"]])

        years_to_predict = req.year - region_df["ds"].dt.year.max()
        if years_to_predict <= 0:
            result[region] = None
            continue

        future = model.make_future_dataframe(periods=years_to_predict, freq="YS")
        forecast = model.predict(future)
        year_pred = forecast[forecast["ds"].dt.year == req.year]
        avg = year_pred["yhat"].mean()

        result[region] = {
            "value": round(avg, 2),
            "source": "forecast"
        }

        existing = db.query(RegionEnergyConsumption).filter_by(region=region, year=req.year).first()
        if not existing:
            db_record = RegionEnergyConsumption(
            region=region,
            year=req.year,
            consumption=float(avg),
            source="forecast"
        )
            db.add(db_record)

    db.commit()  

    return result
