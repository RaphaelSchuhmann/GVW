# Audio DTS Details

## GPIO and Pinmux Configuration

### Audio Pinmux
- aud_dat_miso0/1: Audio data input pins
- vow_dat_miso, vow_clk_miso: Voice wake audio pins
- aud_nle_mosi: Audio noise elimination
- aud_gpio_i2s0/3: I2S interface pins
- Configurations include input-enable, bias-pull-down, input-schmitt-enable

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
