# Lenovo TB336FU Pen

## Overview

The Lenovo TB336FU supports an active stylus for handwriting and drawing. Unlike a passive capacitive stylus, the pen is an active input device that communicates directly with the tablet's digitizer integrated into the display assembly.

The pen functions independently of Bluetooth for normal writing and drawing operations. Disabling Bluetooth does not affect handwriting, indicating that the primary communication path is handled entirely by the display digitizer.

---

# Architecture

```text
                    Lenovo Pen
                         │
         Electromagnetic communication
                         │
                 Display Digitizer
                         │
                 Touch Controller IC
                         │
                   Linux Kernel Driver
                         │
                 Linux Input Subsystem
                         │
                Android Input Framework
                         │
                   Applications
```

The stylus does **not** send drawing data through Android services such as the Lenovo Smart Accessories service. Instead, the digitizer hardware converts the pen signal into standard Linux input events.

---

# Operating Principle

The display continuously emits and monitors an electromagnetic field.

The pen contains active electronics powered by its internal battery. These electronics generate or modulate a signal that can be detected by the digitizer beneath the display.

This allows the tablet to determine:

* Pen position (X/Y)
* Contact state
* Pressure
* Hover position
* Potentially tilt (hardware dependent)

The exact feature set depends on the digitizer hardware.

---

# Hover Detection

Unlike a finger, the pen can be detected before physically touching the display.

```text
Finger
    ↓
Touch required
```

```text
Pen
    ↓
Hover detected
    ↓
Touch
```

Hover allows the operating system to know where the pen is located before contact occurs.

Possible uses include:

* Cursor preview
* Precise positioning
* Palm rejection
* Tool previews

---

# Battery

The pen contains a battery which powers its internal electronics.

Without power the pen is expected to become non-functional because:

* The pen tip is not designed to behave like a normal capacitive finger.
* The digitizer relies on the pen's active electromagnetic signal.

Unlike passive styluses, the battery is therefore considered essential for normal operation.

---

# Linux Perspective

From Linux, the pen should appear as a standard input device.

Typical event types include:

```text
BTN_TOOL_PEN
BTN_TOUCH
ABS_X
ABS_Y
ABS_PRESSURE
ABS_TILT_X
ABS_TILT_Y
```

Applications do not communicate with the pen directly.

Instead they receive events through the Linux input subsystem (`evdev`).

---

# Lenovo Software

The Lenovo Smart Accessories service is likely responsible only for optional vendor-specific functionality such as:

* Battery reporting
* Firmware updates
* Accessory management
* Additional Lenovo features

Normal handwriting and drawing should not depend on these services.

---

# Implications for the GVW Tablet Firmware

This architecture is highly beneficial for the planned Linux-based firmware.

Because the pen is expected to appear as a standard Linux input device, the custom firmware should only require:

* Display driver
* Digitizer driver
* Linux input subsystem (`evdev`/`libinput`)

No Lenovo Android framework components should be required for basic pen functionality.
