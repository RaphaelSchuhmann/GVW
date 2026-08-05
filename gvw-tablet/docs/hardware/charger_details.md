# Charger DTS Details

## I2C Configuration
- sgm41542: Charger IC on i2c6 (primary charger)
- sc89601d: Charger IC on i2c6 (secondary charger)

## Other Peripherals

### USB
- USB Controller: 0x11201000 (OTG mode)
- XHCI Host: 0x11200000
- USB PHY: 0x11e40000 (USB2), 0x11e40700 (USB3)
- USB Type-C: upm7610pd@60 with PD support

## Thermal & Battery Management

### Battery Charger Configuration

#### Primary Charger (sgm41543@1a on I2C3)
- Input voltage limit: 5V (0x4c4b40 = 5000000 µV)
- Input current limit: 3A (0x2dc6c0 = 3000000 µA)
- IRQ GPIO: GPIO3
- IO channel: pmic_vbus

#### Secondary Charger (sc89601d@6b on I2C6)
- Compatible: southchip,sc89601d
- Charger name: secondary_chg
- Vsys min: 0x05
- IPre-charge: 0x02
- Itermination: 0x02
- Vbat volt: 0x2c (44V)
- VAC OVP: 0x03
- Iboost: 0x01
- Vboost: 0x0c

#### Charger Algorithm
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

#### JEITA Temperature Thresholds
- temp_t4_thres: 0x1f4 (500 m°C = 50°C)
- temp_t3_thres: 0x1c2 (450 m°C = 45°C)
- temp_t2_thres: 0x96 (150 m°C = 15°C)
- temp_t1_thres: 0x00 (0°C)
- temp_t0_thres: 0x00 (0°C)
- Min charge temp: 0°C
- Max charge temp: 50°C

#### Battery Thermal Mitigation
- Screen on: 5A, 2A, 1.4A, 1.2A, 1A, 0.5A
- Screen off: 5A, 1A, 0.5A

#### Battery OC Throttling
- OC threshold high: 0x1a90 (6800 mA)
- OC threshold low: 0x1f40 (8000 mA)

#### Low Battery Throttling
- HV threshold: 0xce4 (3300 mV)
- LV1 threshold: 0xc4e (3150 mV)
- LV2 threshold: 0xbb8 (3000 mV)

#### PE/PD Charging
- PE2: Start SOC 0%, Stop SOC 85%
- PD: Stop SOC 80%
- PE45: Stop SOC 80%, High temp enter 39°C, High temp leave 46°C, Low temp leave 10°C, Low temp enter 16°C