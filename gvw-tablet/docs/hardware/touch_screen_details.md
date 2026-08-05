# Touch Screen DTS Details

## GPIO and Pinmux Configuration

### Touch Panel
- IRQ GPIO: GPIO9
- Reset GPIO: GPIO43

## I2C Configuration


## Display Configuration

### Touch Panel
- SPI Interface: SPI3 with touch controller
- Controllers: Himax (hxcommon) or Novatek (NVT-ts-spi)
- Panel Coordinates: 0-16000 (X), 0-25600 (Y)
- Pen Support: Yes (Novatek)

## Other Peripherals

### SPI Controllers
- SPI3: Touch panel interface with extensive configuration