# Energy-Grid-Immersive-Analytics
A UCL IXN project focused on immersive energy grid data analytics.

## Prototype

You can view the Figma prototype here:  
[Figma Prototype →](https://www.figma.com/design/OQmP5Oy1DRHOOuSBoV6fce/Untitled?node-id=1-142&t=QEvBvoJhwFXfmGFQ-1)

## Data Sources

- **Flooding Risk Data**: [Environment Agency Flood Monitoring API](https://environment.data.gov.uk/flood-monitoring/id/floods)
- **Renewable Energy Sites**: [UK Government Renewable Energy Planning Database (REPD)](https://www.gov.uk/government/publications/renewable-energy-planning-database-monthly-extract)
- **Electricity Consumption Statistics**: [Electricity Consumption Statistics](https://www.gov.uk/government/statistics/regional-and-local-authority-electricity-consumption-statistics)
- **Geospatial Boundaries**: [UK NUTS Level 1 Regional Boundaries (ONS, 2018)](https://geoportal.statistics.gov.uk/datasets/44c039e762d94a42bf5e0580e8dd9f84_0/explore?location=55.193166%2C-3.316972%2C6.34)

## Project Structure

### Phone Application
- **Activity**: `app/src/main/java/com/ucl/energygrid/MainActivity.kt`
- **Manifest**: `app/src/phone/AndroidManifest.xml`

### Smart Glasses Application
- **Activity**: `app/src/main/java/com/ucl/energygrid/WearMainActivity.kt`
- **Manifest**: `app/src/wear/AndroidManifest.xml`


## Build Variants

You can switch between **phone** and **wear** variants in Android Studio:

1. Open **Build Variants** panel (View/Tool Windows/Build Variants).
2. In the **Module: app** row, change the **Active Build Variant** to either:
    - `phoneDebug` → builds the Phone app
    - `wearDebug` → builds the Wear app
3. Run or build the project as usual.

### APK Output

The APKs are already built — you can install them directly without building:

- **Phone build**  
  `app/build/outputs/apk/phone/debug/app-phone-debug.apk`

- **Wear build**  
  `app/build/outputs/apk/wear/debug/app-wear-debug.apk`