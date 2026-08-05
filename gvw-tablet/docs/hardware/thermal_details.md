# Full Thermal DTS Details

## Thermal Sensors

### PMIC NTC Sensors (mt6377-tia-ntc)
- thermal-ntc1: Register 0x1c023528
- thermal-ntc2: Register 0x1c023520
- thermal-ntc3: Register 0x1c02351c
- thermal-ntc4: Register 0x1c023530

### Generic ADC Thermal Sensors
- thermal-ntc5: IO channel 0x00 (charger)
- thermal-ntc6: IO channel 0x01 (flash)
- thermal-ntc7: IO channel 0x02 (wifi)
- thermal-ntc8: IO channel 0x03 (usb)
- thermal-ntc9: IO channel 0x05 (board)

### PMIC6377 Thermal Sensor
- pmic6377-thermal: Uses PMIC TS1-TS4 channels
    - IO channels: pmic6377_ts1, pmic6377_ts2, pmic6377_ts3, pmic6377_ts4
    - NVMEM: mt6377_e_data

## Thermal Zones

### CPU Thermal Zones
- soc_max: Critical trip point at 113°C (0x1bb5c = 113000 m°C)
- cpu_big1-4: 4 big core thermal sensors
- cpu_little1-4: 4 little core thermal sensors

### GPU Thermal Zone
- gpu2:
    - Polling delay: 500ms (passive: 300ms)
    - Trip point: 85°C (0x14c08 = 85000 m°C), hysteresis 2°C
    - Cooling device: GPU throttling

### Other Thermal Zones
- gpu1, soc1-4: Additional SoC thermal sensors
- md1-4: Modem thermal sensors
- consys: Connectivity subsystem thermal sensor
- pmic6377_vs1, vbuck2, vemc, vmch: PMIC power rail thermal sensors
- ap_ntc, ltepa_ntc, nrpa_ntc, tsx_ntc: Board NTC sensors
- charger_ntc, flash_ntc, wifi_ntc, usb_ntc, board_ntc: Peripheral NTC sensors

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

## Battery Charger Configuration

### Primary Charger (sgm41543@1a on I2C3)
- Input voltage limit: 5V (0x4c4b40 = 5000000 µV)
- Input current limit: 3A (0x2dc6c0 = 3000000 µA)
- IRQ GPIO: GPIO3
- IO channel: pmic_vbus

### Secondary Charger (sc89601d@6b on I2C6)
- Compatible: southchip,sc89601d
- Charger name: secondary_chg
- Vsys min: 0x05
- IPre-charge: 0x02
- Itermination: 0x02
- Vbat volt: 0x2c (44V)
- VAC OVP: 0x03
- Iboost: 0x01
- Vboost: 0x0c

### Charger Algorithm
- Battery CV: 0x44f840 (4500 mV)
- Max charger voltage: 0xd59f80 (14000 mV)
- Min charger voltage: 0x4630c0 (4500 mV)
- USB charger current: 0x7a120 (500 mA)
- AC charger current: 0x155cc0 (1400 mA)
- AC input current: 0x1e8480 (2000 mA)
- Charging host current: 0x16e360 (1500 mA)
- QC charger current: 0x4c4b40 (5000 mA)
- PD charger current: 0x4c4b40 (5000 mA)
- PD input current: 0x225510 (2200 mA)

### JEITA Temperature Thresholds
- temp_t4_thres: 0x1f4 (500 m°C = 50°C)
- temp_t3_thres: 0x1c2 (450 m°C = 45°C)
- temp_t2_thres: 0x96 (150 m°C = 15°C)
- temp_t1_thres: 0x00 (0°C)
- temp_t0_thres: 0x00 (0°C)
- Min charge temp: 0°C
- Max charge temp: 50°C

### Battery Thermal Mitigation
- Screen on: 5A, 2A, 1.4A, 1.2A, 1A, 0.5A
- Screen off: 5A, 1A, 0.5A

### Battery OC Throttling
- OC threshold high: 0x1a90 (6800 mA)
- OC threshold low: 0x1f40 (8000 mA)

### Low Battery Throttling
- HV threshold: 0xce4 (3300 mV)
- LV1 threshold: 0xc4e (3150 mV)
- LV2 threshold: 0xbb8 (3000 mV)

### PE/PD Charging
- PE2: Start SOC 0%, Stop SOC 85%
- PD: Stop SOC 80%
- PE45: Stop SOC 80%, High temp enter 39°C, High temp leave 46°C, Low temp leave 10°C, Low temp enter 16°C