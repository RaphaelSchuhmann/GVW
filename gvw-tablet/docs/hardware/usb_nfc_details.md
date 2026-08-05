# USB / NFS DTS Details

## GPIO and Pinmux Configuration

### USB Type-C
- Interrupt GPIO: GPIO12

## Other Peripherals

### USB
- USB Controller: 0x11201000 (OTG mode)
- XHCI Host: 0x11200000
- USB PHY: 0x11e40000 (USB2), 0x11e40700 (USB3)
- USB Type-C: upm7610pd@60 with PD support

### NFC
- mt6382nfc: NFC controller (disabled by default)
