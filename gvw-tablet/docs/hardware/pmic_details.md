# PMIC DTS Details

## I2C Device Details
- mt6375: PMIC on i2c1
- rt5133: PMIC/GPIO expander on i2c1 and i2c6

## PMICs
- mt6377: Main PMIC with RTC, fuel gauge, battery throttling
- mt6319: Secondary PMIC with buck regulators
- rt5133: GPIO expander and regulator

## Thermal & Battery Management

### PMIC NTC Sensors (mt6377-tia-ntc)
- thermal-ntc1: Register 0x1c023528
- thermal-ntc2: Register 0x1c023520
- thermal-ntc3: Register 0x1c02351c
- thermal-ntc4: Register 0x1c023530

### PMIC6377 Thermal Sensor
- pmic6377-thermal: Uses PMIC TS1-TS4 channels
    - IO channels: pmic6377_ts1, pmic6377_ts2, pmic6377_ts3, pmic6377_ts4
    - NVMEM: mt6377_e_data

### Fuel Gauge (mt6377-gauge)
- Boot mode: 0x46
- IO channels: pmic_battery_temp, pmic_battery_voltage, pmic_bif_voltage, pmic_ptim_voltage, pmic_ptim_r
- NVMEM: initialization, state-of-charge
- Shutdown voltage: 0x84d0 (34000 mV)
- Temperature thresholds: T0=50°C, T1=25°C, T2=10°C, T3=0°C, T4=-6°C, T5=-10°C
- Battery profiles: 6 temperature profiles (t0-t5) with 100 entries each (OCV vs capacity)
- Q_MAX_SYS_VOLTAGE: 0xd16 (3350 mV)
- FG meter resistance: 0x46 (70 mΩ)
- PMIC shutdown current: 0x14 (20 mA)
- PMIC min voltage: 0x82dc (33500 mV)
- Multi-temp gauge: Enabled