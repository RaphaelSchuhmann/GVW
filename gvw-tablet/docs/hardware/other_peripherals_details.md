# Full Other Peripherals DTS Details

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

