import os
import pytest
import pandas as pd
import numpy as np
from prophet import Prophet

# -------------------- 誤差計算函數 --------------------
def mae(y_true, y_pred):
    return np.mean(np.abs(y_true - y_pred))

def rmse(y_true, y_pred):
    return np.sqrt(np.mean((y_true - y_pred) ** 2))

def mape(y_true, y_pred):
    y_true, y_pred = np.array(y_true), np.array(y_pred)
    mask = y_true != 0
    return np.mean(np.abs((y_true[mask] - y_pred[mask]) / y_true[mask]))

# -------------------- 測試參數 --------------------
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

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.normpath(os.path.join(BASE_DIR, "..", "..", "app", "src", "main", "assets", "Subnational_electricity_consumption_statistics"))

# -------------------- 讀取所有年份 CSV，合併 --------------------
def load_all_years():
    all_years = range(2005, 2024)
    

    df_list = []
    for year in all_years:
        file_path = os.path.join(
            data_dir,
            f"Subnational_electricity_consumption_statistics_{year}.csv"
        )
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"CSV not found: {file_path}")
        year_df = pd.read_csv(file_path)
        year_df["year"] = year
        df_list.append(year_df)
    df_all = pd.concat(df_list, ignore_index=True)
    
    # 對應 region code
    df_all["region"] = df_all["Code"].map(code_map)
    
    # 選取你想用的消耗欄位
    consumption_col = 'Total consumption\n(GWh):\nAll Domestic'
    df_all["consumption"] = df_all[consumption_col]
    
    # 保留必要欄位
    df_all = df_all[["year", "region", "consumption"]]
    return df_all

df_all = load_all_years()

# -------------------- Prophet 測試 --------------------
@pytest.mark.parametrize("region", target_regions)
def test_prophet_train_test_split(region):
    region_df = df_all[df_all["region"] == region].copy()

    # 訓練集 2005-2020，測試集 2021-2023
    train_df = region_df[region_df["year"] <= 2020].copy()
    test_df = region_df[region_df["year"] > 2020].copy()

    # Prophet 格式
    train_df["ds"] = pd.to_datetime(train_df["year"], format="%Y")
    train_df["y"] = train_df["consumption"].astype(float)

    # 建立並訓練模型
    model = Prophet(yearly_seasonality=False)
    model.fit(train_df[["ds", "y"]])

    # 預測 2021–2023
    future = model.make_future_dataframe(periods=3, freq="YS")
    forecast = model.predict(future)
    forecast["year"] = forecast["ds"].dt.year

    # 合併測試集
    preds = forecast[forecast["year"].isin(test_df["year"])][["year", "yhat"]]
    merged = pd.merge(test_df, preds, on="year")

    mae_val = mae(merged["consumption"], merged["yhat"])
    mape_val = mape(merged["consumption"], merged["yhat"])
    rmse_val = rmse(merged["consumption"], merged["yhat"])

    print(f"{region} → MAE={mae_val:.2f}, MAPE={mape_val:.2%}, RMSE={rmse_val:.2f}")
    assert mape_val < 0.3


