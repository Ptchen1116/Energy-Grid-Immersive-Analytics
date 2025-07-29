from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.energy_site import EnergySite  
import csv
from pyproj import Transformer
from pathlib import Path
from pathlib import Path

router = APIRouter()

BASE_DIR = Path(__file__).resolve().parent
BASE_DIR = Path(__file__).resolve().parent
CSV_PATH = (BASE_DIR / "../src/main/assets/renewable_energy_planning_database.csv").resolve()

@router.get("/sites/{category}")
def get_sites(category: str, db: Session = Depends(get_db)):
    existing = db.query(EnergySite).filter(EnergySite.type == category).all()
    if existing:
        return [{"name": s.name, "lat": s.latitude, "lon": s.longitude} for s in existing]

    category_map = {
        "solar": {"Solar Photovoltaics"},
        "wind": {"Wind Offshore", "Wind Onshore"},
        "hydroelectric": {"Large Hydro", "Small Hydro", "Pumped Storage Hydroelectricity"}
    }
    target_types = category_map.get(category.lower(), set())
    if not target_types:
        return []

    transformer = Transformer.from_crs("EPSG:27700", "EPSG:4326", always_xy=True)
    results = []

    with CSV_PATH.open("r", encoding="latin1") as f:
        reader = csv.DictReader(f)
        for row in reader:
            tech_type = row.get("Technology Type", "").strip()
            status = row.get("Development Status (short)", "").strip()
            site_name = row.get("Site Name", "").strip()
            try:
                x = float(row["X-coordinate"])
                y = float(row["Y-coordinate"])
            except (ValueError, TypeError):
                continue

            if tech_type in target_types and status == "Operational":
                lon, lat = transformer.transform(x, y)
                results.append({"name": site_name, "lat": lat, "lon": lon})

                site = EnergySite(
                    name=site_name,
                    type=category,
                    latitude=lat,
                    longitude=lon
                )
                db.add(site)

    db.commit()
    return results