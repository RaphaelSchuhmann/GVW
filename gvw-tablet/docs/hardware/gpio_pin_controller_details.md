# GPIO Pin Controller DTS Details

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
