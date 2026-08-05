# Display DTS Details

## GPIO and Pinmux Configuration

### Display (LCM)
- Reset GPIO: GPIO86 (lcm_rst_out1), GPIO85 (lcm_rst_out0)
- DSI TE GPIO: GPIO85

## Display Configurat

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
