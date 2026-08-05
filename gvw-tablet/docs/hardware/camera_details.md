# Camera DTS Details

## I2C Configuration

### I2C Controllers (12 total)

| Alias | Address    | Devices Attached                                     |
|-------|------------|------------------------------------------------------|
| i2c0  | 0x11ed0000 | -                                                    |
| i2c1  | 0x11db0000 | -                                                    |
| i2c2  | 0x11ed1000 | -                                                    |
| i2c3  | 0x11b20000 | -                                                    |
| i2c4  | 0x11ed2000 | camera_eeprom1@51, camera_sub@1a                     |
| i2c5  | 0x11b21000 | -                                                    |
| i2c6  | 0x11db1000 | gate_ic@11                                           |
| i2c7  | 0x11db2000 | -                                                    |
| i2c8  | 0x11db3000 | camera_eeprom0@50, camera_main@1a, camera_main_af@18 |
| i2c9  | 0x11ed3000 | -                                                    |
| i2c10 | 0x11280000 | -                                                    |
| i2c11 | 0x11281000 | -                                                    |

### I2C Device Details
- camera_eeprom0, camera_eeprom1: Camera calibration EEPROMs
- camera_main: Main camera sensor
- camera_sub: Sub/secondary camera sensor
- camera_main_af: Main camera autofocus
- gate_ic: Gate IC for camera power control

## GPIO and Pinmux Configuration

### Camera Flashlight
- GPIO147, GPIO148, GPIO171: Flash control with PWM and GPIO modes
- Multiple pinmux configurations for different drive strengths and output states

### Camera Reset Pinmux
- cam0_rst0/1, cam1_rst0/1, cam2_rst0/1: Camera reset control
- cam0_mclk_2/4/6/8mA: Camera master clock with different drive strengths

## Camera Configuration

### Camera Hardware Node
- Address: 0x1a004000
- Compatible: mediatek,imgsensor
- Pinctrl States: Multiple states for reset, MCLK drive strengths, and AVDD GPIO control

### Camera Sensors
- camera_main@1a: Main rear camera (I2C8)
- camera_sub@1a: Sub/secondary camera (I2C4)
- camera_main_af@18: Main camera autofocus (I2C8)
- camera_af_hw_node: AF lens hardware with PMIC supply

### Camera EEPROMs
- camera_eeprom0@50: Main camera calibration data (I2C8)
- camera_eeprom1@51: Sub camera calibration data (I2C4)

### Camera Power Supplies
- cam0_vcama, cam0_vcamd, cam0_vcamio, cam0_vcamaf: Main camera power rails
- cam1_vcama, cam1_vcamd, cam1_vcamio: Sub camera power rails

### Camera Flash
- lm3643@63: LED flash driver on I2C6
- Flash Enable GPIO: GPIO148
- Flash Select GPIO: GPIO171
- PWM Control: Available on GPIO147, GPIO148, GPIO171
