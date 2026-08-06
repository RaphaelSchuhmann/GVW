# Lenovo TB336FU Components

---

# Linux Support Status Definitions

| Status      | Meaning                                                                    |
| ----------- | -------------------------------------------------------------------------- |
| Full        | Fully supported in Linux mainline                                          |
| Partial     | Mainline driver exists, but hardware support or features may be incomplete |
| Pending     | Driver or patches exist but are not merged into mainline                   |
| Vendor Only | Android/vendor BSP driver only                                             |
| Unknown     | Requires further investigation                                             |

---

# System Overview

* Platform: MediaTek MT6835 / MT8755
* Device: Lenovo TB336FU tablet

The MediaTek MT6835/MT8755 platform is the primary application platform containing CPU cores, GPU, memory controller, display, camera, audio, and connectivity subsystems.

---

# Components

## SoC

**Chip:** MediaTek MT6835 / MT8755
**Interface:** Internal SoC buses
**Kernel Driver:** MediaTek vendor BSP
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Primary application processor containing CPU, GPU, memory controller, and integrated peripherals.
**DTS Details:** [SoC DTS](./hardware/soc_details.md)

---

# PMICs (Power Management)

## Main PMIC

**Chip:** MediaTek MT6377
**Interface:** I²C
**Kernel Driver:** MediaTek MT6377 vendor drivers
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Main power management IC providing voltage regulators, RTC, battery gauge, thermal monitoring, charging management, and integrated audio codec functionality.
**DTS Details:** [PMIC DTS](./hardware/pmic_details.md)

---

## Secondary PMIC

**Chip:** MediaTek MT6319
**Interface:** I²C
**Kernel Driver:** MediaTek MT6319 vendor drivers
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Secondary PMIC providing additional buck regulators and power rails.
**DTS Details:** [PMIC DTS](./hardware/pmic_details.md)

---

## GPIO Expander / Regulator

**Chip:** Renesas RT5133
**Interface:** I²C
**Kernel Driver:** regulator-rt5133
**Kernel Config:** CONFIG_REGULATOR_RT5133
**Upstream Status:** Mainline driver available
**Mainline Support:** Full
**Description:** PMIC providing 8 LDO regulators and 3 GPIO outputs used for peripheral power control, including camera-related power rails.
**DTS Details:** [GPIO Expander/Regulator DTS](./hardware/gpio_pin_controller_details.md)

---

# Battery Charging

## Primary Charger

**Chip:** SGM41542
**Interface:** I²C
**Kernel Driver:** sgm41542 charger driver
**Upstream Status:** Mainline driver available
**Mainline Support:** Full
**Description:** Single-cell Li-ion buck charger with NVDC power path management and up to 3A charging current.
**DTS Details:** [Charger DTS](./hardware/charger_details.md)

---

## Secondary Charger

**Chip:** Southchip SC89601D
**Interface:** I²C
**Kernel Driver:** sc89601d charger driver
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Secondary synchronous buck charger controlled through I²C.
**DTS Details:** [Charger DTS](./hardware/charger_details.md)

---

# Audio Subsystem

## Audio Codec

**Chip:** MediaTek MT6377 codec (integrated into MT6377 PMIC)
**Interface:** Internal PMIC connection
**Kernel Driver:** MediaTek MT6377 audio codec driver
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Integrated audio codec providing DAC, ADC, headphone output, and DMIC microphone support. The DTS indicates support for three microphone inputs.
**DTS Details:** [Audio DTS](./hardware/audio_details.md)

---

## Speaker Amplifier

**Chip:** Richtek RT5512
**Interface:** I²C
**Kernel Driver:** snd-soc-rt5512
**Upstream Status:** Upstream work exists, not fully merged
**Mainline Support:** Pending
**Description:** Smart class-D speaker amplifier with voltage/current sensing, digital signal processing, dynamic range control, and speaker protection features.
**DTS Details:** [Audio DTS](./hardware/audio_details.md)

---

# Touch Screen

## Touch Controller (Himax)

**Chip:** Himax hxcommon
**Interface:** I²C
**Kernel Driver:** Himax touchscreen driver
**Upstream Status:** Downstream/vendor driver
**Mainline Support:** Vendor Only
**Description:** Capacitive touchscreen controller supporting multi-touch input. Present in the DTS as a possible supported controller variant.
**DTS Details:** [Touch Screen DTS](./hardware/touch_screen_details.md)

---

## Touch Controller (Novatek)

**Chip:** Novatek NVT-ts-spi
**Interface:** SPI / I²C
**Kernel Driver:** Novatek touchscreen driver
**Upstream Status:** Partial upstream support depending on controller model
**Mainline Support:** Partial
**Description:** Touch controller with stylus support. Likely candidate for the active pen functionality of the device.
**DTS Details:** [Touch Screen DTS](./hardware/touch_screen_details.md)

---

# Camera

## LED Flash Driver

**Chip:** LM3643
**Interface:** I²C
**Kernel Driver:** lm3643 LED driver
**Upstream Status:** Android/vendor support
**Mainline Support:** Vendor Only
**Description:** Dual LED flash controller with I²C control and up to 1.5A total LED current output.
**DTS Details:** [Camera DTS](./hardware/camera_details.md)

---

# USB / Connectivity

## USB Type-C Power Delivery Controller

**Chip:** UPM7610PD
**Interface:** I²C
**Kernel Driver:** Unknown
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** USB Type-C controller providing Power Delivery functionality and Type-C port management.
**DTS Details:** [USB/NFC DTS](./hardware/usb_nfc_details.md)

---

## NFC Controller

**Chip:** MediaTek MT6382NFC
**Interface:** Unknown
**Kernel Driver:** MediaTek NFC vendor driver
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** NFC controller. Disabled by default in the device tree.
**DTS Details:** [USB/NFC DTS](./hardware/usb_nfc_details.md)

---

# Display

## DSI Controller

**Chip:** MediaTek MT6835 DSI
**Interface:** MIPI DSI
**Kernel Driver:** mediatek-drm / MediaTek DSI driver
**Upstream Status:** Mainline MediaTek DRM support exists, MT6835-specific support incomplete
**Mainline Support:** Partial
**Description:** MIPI DSI display controller responsible for communication between the SoC and the LCD panel.
**DTS Details:** [Display DTS](./hardware/display_details.md)

---

## Display Panel

**Chip:** panel,sycamore
**Interface:** MIPI DSI
**Kernel Driver:** Vendor-specific panel driver
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Device-specific MIPI DSI display panel configuration containing timing, reset, and power sequence information.
**DTS Details:** [Display DTS](./hardware/display_details.md)

---

# GPIO Pin Controller

**Chip:** MediaTek MT6835 pinctrl
**Interface:** Internal SoC peripheral bus
**Kernel Driver:** mediatek pinctrl driver
**Upstream Status:** Mainline MediaTek pinctrl support exists
**Mainline Support:** Partial
**Description:** GPIO and pin multiplexing controller providing control over 193 GPIO pins and their alternate functions.
**DTS Details:** [GPIO Pin Controller DTS](./hardware/gpio_pin_controller_details.md)

---

# Connectivity (WiFi / Bluetooth / GPS)

## Connectivity Subsystem

**Chip:** MediaTek Connectivity Subsystem (MT6835)
**Interface:** Internal MediaTek connectivity bus
**Kernel Driver:** MediaTek MTK_COMBO
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** Combined wireless subsystem providing WiFi, Bluetooth, and GNSS connectivity functionality.
**DTS Details:** [Connectivity Subsystem DTS](./hardware/connectivity_details.md)

---

## GPS / GNSS

**Chip:** MediaTek GNSS subsystem
**Interface:** Internal connectivity subsystem
**Kernel Driver:** MediaTek GPS driver
**Upstream Status:** Vendor BSP only
**Mainline Support:** Vendor Only
**Description:** GNSS receiver supporting L1 and L5 bands with external LNA control through GPIO.
**DTS Details:** [GPS DTS](./hardware/connectivity_details.md)

---

# Additional Details

* [Thermal DTS](./hardware/thermal_details.md)
* [Other Peripherals DTS](./hardware/other_peripherals_details.md)

---

# Full DTS Documentation

The complete DTS analysis containing memory regions, GPIO mappings, pinmux configuration, regulators, thermal zones, and peripheral details is available here:

[Full DTS Documentation](./hardware/Full_DTS_Documentation.md)

Full DTS file: [lenovo_tb336fu.dts](hardware/dts/lenovo_tb336fu.dts)