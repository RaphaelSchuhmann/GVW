# TB336FU Boot Process
## Overview
The Lenovo TB336FU follows the standard secure MediaTek Android boot chain. Each stage
performs hardware initialization, verifies the integrity of the next stage, and transfers execution until
Android userspace is started.
```
Power On
|
▼
BootROM (SoC ROM)
|
▼
Preloader
|
▼
Little Kernel (LK)
|
├── Android Verified Boot (AVB) -> LK performs verification
|
▼
boot.img
|
├── init_boot.img
├── vendor_boot.img
└── dtbo.img
|
▼
Linux Kernel
|
▼
init (PID 1)
|
▼
Android Framework
|
▼
Launcher / Applications
```

## 1. BootROM

### Responsibilities
- Executes immediately after the SoC receives power
- Performs minimal hardware initialization
- Initializes enough SRAM to execute the next stage
- Selects the boot device (UFS on the TB336FU)
- Loads the MediaTek Preloader
- Performs the initial secure boot verification (if enabled)
- Cannot be modified because it is permanently stored in the SoC

### Loads
```
preloader_a
```
or
```
preloader_b
```
depending on the active slot.

## 2. Preloader

### Responsibilities
The Preloader is the first software that can be updated by Lenovo.

It is responsible for:
- Initializing DRAM
- Initializing clocks
- Initializing PMIC
- Configuring watchdog
- Bringing basic peripherals online
- Preparing the execution environment
- Loading Little Kernel

Without a functioning Preloader, the device cannot even access external RAM.

### Loads
```
lk_a / lk_b
```

## 3. Little Kernel

### Responsibilities
LK is the Android bootloader.

Its responsibilities include:
- Fastboot implementation
- Recovery Mode
- Slot selection (A/B)
- Loading boot images
- Passing boot arguments
- Loading the Linux kernel

For the TB336FU it also interacts with:
- vbmeta
- dtbo
- vendor_boot
- init_boot

## 4. Android Boot Images
Before Linux starts, several images participate in the boot process.

**boot.img**
Contains:
- Linux kernel
- Generic boot ramdisk (minimal; Android initialization primarily uses init_boot.img on modern GKI devices)

**init_boot.img**
Contains:
- Generic Android init ramdisk

Introduced with Android GKI

**vendor_boot.img**
Contains:
- Vendor ramdisk
- First-stage init fragments
- Hardware initialization configuration
- fstab
- ueventd configuration

**dtbo.img**
Contains:
- Device Tree Overlays

These describe board-specific hardware differences.

## 5. Linux Kernel

### Responsibilities
- Initialize scheduler
- Initialize memory management
- Initialize interrupt controller
- Initialize device drivers
- Parse the Device Tree (DTB) and apply Device Tree Overlays (DTBO)
- Load kernel modules (`vendor_dlkm`, `odm_dlkm`, `system_dlkm`)
- Mount the initial root filesystem
- Start userspace

When initialization completes:
```
execve("/init")
```

is executed.

## 6. Android Init

Android's `init` process runs as **PID 1**.

### Responsibilities
- Parse init.rc
- Start first-stage init
- Mount partitions
- Initialize SELinux
- Configure properties
- Start system daemons
- Unlock File-Based Encryption when credentials become available
- Start Zygote

Important services started include:
- vold
- servicemanager
- hwservicemanager
- logd
- apexd

## 7. Android Framework

After Zygote is started:
- System Server launches
- Binder services become available
- HALs are initialized
- Framework services start
- Launcher starts
- User applications can execute

This stage represents the transition from os startup to normal Android runtime.

## TB336FU-Specific Notes
TB336FU:
- Uses MediaTek MT6835 (Dimensity 6300) platform.
- Uses A/B seamless updates.
- Uses AVB 2.0
- Uses dynamic partitions stored in `super`
- Uses EROFS for system partitions
- Uses F2FS for `userdata`
- Uses `vendor_dlkm`, `odm_dlkm`, and `system_dlkm` for loadable kernel modules.
- Uses Device Tree Overlays (`dtbo`) for board-specific hardware configuration.
