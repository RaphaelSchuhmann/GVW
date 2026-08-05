# Full DTS Documentation

Full DTS file [lenovo_tb336fu.dts](dts/lenovo_tb336fu.dts)

## System Overview
- Platform: MediaTek MT6835/MT8755 SoC
- Device: Lenovo TB336FU tablet
- File Path: lenovo_tb336fu.dts
- CPU and Memory Configuration

### Memory Blocks
Multiple reserved memory regions defined for various subsystems:

- MCUPM, SSPM, gz-log, unmap, platform_mtksmmu_protpgd, gz, me_cmdq_reserved: Reserved for firmware and secure processing
- framebuffer: Display framebuffer memory
- atf-log-reserved, log_store: Logging buffers
- emi_mbist_buf, dramc-rk1, dramc-rk0: DRAM testing and calibration
- aee_lk, BL31-reserved, minirdump, pstore, aee_debug_kinfo: Debug and crash dump memory

### CMA (Contiguous Memory Allocator) Pools
- ssmr-reserved-cma_memory: Shared DMA pool for SSMR
- ssheap-reserved-cma_memory: Shared heap memory
- ccci-dpmaif-cache-memory: CCCI DPMAIF cache
- ccci-dpmaif-nocache-memory: CCCI DPMAIF non-cacheable memory
- cmdq-resv-memory: Command queue reserved memory

### Memory Features
memory-ssmr-features: SSMR feature configuration
ssmr, ssheap: Memory subsystem configurations
drm-wv: DRM Widevine support
mtee-svp: Secure video path support

---

## I2C Configuration

### I2C Controllers (12 total)

| Alias | Address    | Devices Attached                                     |
|-------|------------|------------------------------------------------------|
| i2c0  | 0x11ed0000 | -                                                    |
| i2c1  | 0x11db0000 | mt6375 PMIC, rt5133, fs15xx, ps5169                  |
| i2c2  | 0x11ed1000 | -                                                    |
| i2c3  | 0x11b20000 | -                                                    |
| i2c4  | 0x11ed2000 | camera_eeprom1@51, camera_sub@1a                     |
| i2c5  | 0x11b21000 | -                                                    |
| i2c6  | 0x11db1000 | sgm41542@1a, sc89601d@6b, rt5133@18, gate_ic@11      |
| i2c7  | 0x11db2000 | -                                                    |
| i2c8  | 0x11db3000 | camera_eeprom0@50, camera_main@1a, camera_main_af@18 |
| i2c9  | 0x11ed3000 | -                                                    |
| i2c10 | 0x11280000 | -                                                    |
| i2c11 | 0x11281000 | -                                                    |	

### I2C Device Details
- mt6375: PMIC on i2c1
- rt5133: PMIC/GPIO expander on i2c1 and i2c6
- sgm41542: Charger IC on i2c6
- sc89601d: Charger IC on i2c6
- camera_eeprom0, camera_eeprom1: Camera calibration EEPROMs
- camera_main: Main camera sensor
- camera_sub: Sub/secondary camera sensor
- camera_main_af: Main camera autofocus
- gate_ic: Gate IC for camera power control

---

## GPIO and Pinmux Configuration

### GPIO Controller
- Compatible: mediatek,gpio / mediatek,mt6835-pinctrl
- Base Address: 0x10005000
- GPIO Count: 193 pins (0xc1)
- Register Regions: gpio, iocfg_lm, iocfg_rb, iocfg_bl, iocfg_bm, iocfg_br, iocfg_lt, iocfg_rm, iocfg_rt

### **Key GPIO Configurations**

### Hall Sensor (Magnetic Cover Detection)
- GPIOs: GPIO15 (irq1), GPIO18 (irq2)
- Interrupts: 0x0f 0x08, 0x12 0x08

### Touch Panel
- IRQ GPIO: GPIO9
- Reset GPIO: GPIO43

### Lenovo Keyboard
- UART TX GPIO: GPIO52
- UART RX GPIO: GPIO53
- Power Enable GPIO: GPIO38
- Plug GPIO: GPIO7
- UART Wake GPIO: GPIO92
- TX Enable GPIO: GPIO124

### Camera Flashlight
- GPIO147, GPIO148, GPIO171: Flash control with PWM and GPIO modes
- Multiple pinmux configurations for different drive strengths and output states

### Display (LCM)
- Reset GPIO: GPIO86 (lcm_rst_out1), GPIO85 (lcm_rst_out0)
- DSI TE GPIO: GPIO85

### GPS LNA Control
- L1 LNA: GPIO150 (gps_l1_lna@0)
- L5 LNA: GPIO151 (gps_l5_lna@0)

### USB Type-C
- Interrupt GPIO: GPIO12

### SD Card
- Card Detect GPIO: GPIO14

### **Pinmux Configurations**

### Audio Pinmux
- aud_dat_miso0/1: Audio data input pins
- vow_dat_miso, vow_clk_miso: Voice wake audio pins
- aud_nle_mosi: Audio noise elimination
- aud_gpio_i2s0/3: I2S interface pins
- Configurations include input-enable, bias-pull-down, input-schmitt-enable

### MMC/SD Card Pinmux
- mmc0default: Drive strength 4, pull-up on CMD/DAT, pull-down on CLK
- mmc1default: Drive strength 3, pull-up on CMD/DAT, pull-down on CLK

### UART Pinmux
- uart_tx_set, uart_tx_clear: UART TX control
- uart_rx_set, uart_rx_clear: UART RX control
- uart_rx_gpio, uart_wake_gpio: GPIO mode configurations

### Camera Reset Pinmux
- cam0_rst0/1, cam1_rst0/1, cam2_rst0/1: Camera reset control
- cam0_mclk_2/4/6/8mA: Camera master clock with different drive strengths

---

## Display Configuration

### DSI Controller
- Address: 0x14017000
- Compatible: mediatek,dsi0, mediatek,mt6835-dsi
- Panel: panel,sycamore

#### Panel GPIOs:
- PM Enable: GPIO87
- Reset: GPIO86
- Bias: GPIO150, GPIO151

### Display Components
- dsi_te: DSI TE (Tearing Effect) interrupt on GPIO85
- disp_wdma0@14018000: Display write DMA
- disp_dbpi0@1401e000: Display DBPI interface
- mtkfb@0: MediaTek framebuffer driver
- inlinerot0@14020000: Inline rotation engine

### IOMMU for Display
- Main IOMMU: 0x1e802000 (mediatek,mt6835-disp-iommu)
- IOMMU Banks: 4 banks (0x1e803000 - 0x1e806000)
- LARBs: Multiple local arbiters connected

### Touch Panel
- SPI Interface: SPI3 with touch controller
- Controllers: Himax (hxcommon) or Novatek (NVT-ts-spi)
- Panel Coordinates: 0-16000 (X), 0-25600 (Y)
- Pen Support: Yes (Novatek)

---

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

---

## Other Peripherals

### PMICs
- mt6377: Main PMIC with RTC, fuel gauge, battery throttling
- mt6319: Secondary PMIC with buck regulators
- rt5133: GPIO expander and regulator

### Regulators
- VFP, VTP: Various voltage regulators
- VBUCK3_SSHUB, VSRAM_OTHERS_SSHUB: Hub power
- VMCH_EINT_HIGH/LOW: Memory card power

### USB
- USB Controller: 0x11201000 (OTG mode)
- XHCI Host: 0x11200000
- USB PHY: 0x11e40000 (USB2), 0x11e40700 (USB3)
- USB Type-C: upm7610pd@60 with PD support

### NFC
- mt6382nfc: NFC controller (disabled by default)

### SPI Controllers
- SPI3: Touch panel interface with extensive configuration

### UART Controllers
- Multiple UART instances for console, keyboard, and debug

### DMA Controllers
- dma-controller: UART DMA
- MDP DMA: Media processing DMA channels

### Memory Protection
- device_mpu_low, infra_device_mpu: Memory protection units
- infracfg_mem, apcldmain/out/misc, mdcldmain/out/misc: DMA and memory configuration

### Security Features
- DRM Widevine (drm-wv): Content protection
- Secure Video Path (mtee-svp): Trusted execution for video
- IOMMU: Memory isolation for display and camera subsystems
- Secure memory regions: ATF, MCUPM, SSPM reservations

---

## Audio / Sound Subsystem

### Audio Codec (PMIC Integrated)
- mt6377codec: mediatek,mt6377-sound
    - DMIC mode: 0x00
    - Mic type: 0x01 0x01 0x01 (3 microphones)
    - IO channels: pmic_hpofs_cal
    - NVMEM: pmic-hp-efuse
    - Supply: reg-vaud28

### Speaker Amplifier
- rt5512@5c (I2C6): richtek,rt5512
    - Sound name prefix: "Left"
    - Sound DAI cells: 0x00

### Audio Front End (AFE)
- mt6835-afe-pcm@11050000: mediatek,mt6835-sound
    - Register: 0x11050000, size 0x2000
    - Interrupt: 0x188
    - Power domain: 0x03 (audio)
    - 16 pinctrl states for audio pinmux (aud_clk_mosi, aud_dat_mosi, aud_dat_miso, aud_gpio_i2s, vow_dat_miso, vow_clk_miso)
    - 31 clocks including aud_afe_clk, aud_dac_clk, aud_adc_clk, aud_i2s1-5_bclk, aud_apll1/2, etc.

### Audio SRAM
audio_sram@11052000: mediatek,audio_sram
Size: 0xd000 (52KB)
Prefer mode: 0x00
Mode size: 0x9c00 0xd000

### SCP Audio Processing
- scp_audio_mbox: mediatek,scp_audio_mbox
    - Mailbox for SCP audio communication
    - Interrupt: 0x20a
- snd_scp_audio: mediatek,snd_scp_audio
    - SCP speaker processing enable: 0x00 0x04 0x10 0x15
- snd-scp-ultra: mediatek,snd-scp-ultra
    - DL memif ID: 0x07
    - UL memif ID: 0x0f

### Sound Card
- sound: mediatek,mt6835-mt6377-sound
    - Platform: mt6835-afe-pcm
    - SCP audio: snd_scp_audio
    - Speaker I2S: 0x03 0x00
    - Speaker codec DAI: rt5512@5c

### Bluetooth Audio
- mtk-btcvsd-snd@18050000: mediatek,mtk-btcvsd-snd
    - Register: 0x18050000, 0x18080000
    - Interrupt: 0x82
    - Disable write silence: 0x01

### Smart PA Manager
- frsm_amp_mngr: audio,spkr-amp-mngr
    - Number of amp devices: 0x04
- smart_pa: Interrupt on GPIO13

---

## Thermal & Battery Management

### Thermal Sensors

#### PMIC NTC Sensors (mt6377-tia-ntc)
- thermal-ntc1: Register 0x1c023528
- thermal-ntc2: Register 0x1c023520
- thermal-ntc3: Register 0x1c02351c
- thermal-ntc4: Register 0x1c023530

#### Generic ADC Thermal Sensors
- thermal-ntc5: IO channel 0x00 (charger)
- thermal-ntc6: IO channel 0x01 (flash)
- thermal-ntc7: IO channel 0x02 (wifi)
- thermal-ntc8: IO channel 0x03 (usb)
- thermal-ntc9: IO channel 0x05 (board)

#### PMIC6377 Thermal Sensor
- pmic6377-thermal: Uses PMIC TS1-TS4 channels
    - IO channels: pmic6377_ts1, pmic6377_ts2, pmic6377_ts3, pmic6377_ts4
    - NVMEM: mt6377_e_data

### Thermal Zones

#### CPU Thermal Zones
- soc_max: Critical trip point at 113°C (0x1bb5c = 113000 m°C)
- cpu_big1-4: 4 big core thermal sensors
- cpu_little1-4: 4 little core thermal sensors

#### GPU Thermal Zone
- gpu2:
    - Polling delay: 500ms (passive: 300ms)
    - Trip point: 85°C (0x14c08 = 85000 m°C), hysteresis 2°C
    - Cooling device: GPU throttling

#### Other Thermal Zones
- gpu1, soc1-4: Additional SoC thermal sensors
- md1-4: Modem thermal sensors
- consys: Connectivity subsystem thermal sensor
- pmic6377_vs1, vbuck2, vemc, vmch: PMIC power rail thermal sensors
- ap_ntc, ltepa_ntc, nrpa_ntc, tsx_ntc: Board NTC sensors
- charger_ntc, flash_ntc, wifi_ntc, usb_ntc, board_ntc: Peripheral NTC sensors

#### Fuel Gauge (mt6377-gauge)
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

---

## Connectivity (Wi-Fi / Bluetooth / GNSS)

### Wi-Fi
- wifi@18000000: mediatek,wifi
    - Register: 0x18000000, size 0x100000 (1MB)
    - Interrupt: 0x83
    - EMI address: 0x00
    - EMI size: 0x600000 (6MB)
    - EMI alignment: 0x1000000 (16MB)
    - EMI max address: 0xc0000000
    - Memory region: mblock-32-shared-dma-pool_wifi-reserve-memory_dma (0x7d800000, 6MB)

#### Connectivity Subsystem (Consys)
- consys@18002000: mediatek,mt6835-consys
    - Multiple register regions:
        - 0x18002000 (4KB)
        - 0x1c007000 (256B)
        - 0x10001000 (4KB)
        - 0x1c001000 (4KB)
        - 0x18007000 (4KB)
        - 0x180b1000 (4KB)
        - 0x180a3000 (4KB)
        - 0x180a5000 (2KB)
        - 0x180c1000 (4KB)
        - 0x18004000 (4KB)
        - 0x1024c000 (64B)
- Interrupts: 0x81, 0x80, 0x84
- Power domain: 0x01 (conn)
- EMI address: 0x7d000000
- EMI size: 0x800000 (8MB)
- EMI alignment: 0x1000000 (16MB)
- EMI max address: 0x80000000
- Clock: ccif
- Thermal sensor: Yes (#thermal-sensor-cells = 0x00)
- Reserved memory: mblock-31-consys_emi_reserved (0x7d000000, 8MB)

#### GNSS (GPS)
- gps@18c00000: mediatek,gps
- Register: 0x18c00000
- EMI region: 0x1d
- EMI offset: 0x650000
- EMI size: 0xfffff (1MB)
- EMI domain AP: 0x00
- EMI domain conn: 0x02
- GNSS PMIC: 0x18e9
- Pinctrl states:
    - gps_l1_lna_disable, gps_l1_lna_dsp_ctrl, gps_l1_lna_enable
    - gps_l5_lna_disable, gps_l5_lna_dsp_ctrl, gps_l5_lna_enable
- GPIOs: GPIO150 (L1 LNA), GPIO151 (L5 LNA)
- Status: okay

### Bluetooth
- btif@1100c000: Bluetooth interface controller
- mtk-btcvsd-snd@18050000: Bluetooth CVSD audio sound interface
    - Used for SCO audio over Bluetooth

#### Source Clock RC (srclken-rc)
- srclken-rc@1c00d000: mediatek,srclken-rc
    - Controls XO (crystal oscillator) for various subsystems
    - Subsystems: suspend, md1, md2, md3, rf, ufs, gps, bt, wf, mcu, spm, nfc, coant, rsv
    - GPS control: XO_WCN
    - MCU control: XO_WCN
    - BT control: XO_WCN
    - WF (Wi-Fi) control: XO_WCN
    - NFC control: XO_NFC