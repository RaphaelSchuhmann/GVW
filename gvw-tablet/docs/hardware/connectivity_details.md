# Connectivity DTS Details

## GPIO and Pinmux Configuration -> GPS LNA Control
- L1 LNA: GPIO150 (gps_l1_lna@0)
- L5 LNA: GPIO151 (gps_l5_lna@0)

## Connectivity (WiFi / Bluetooth / GNSS)

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