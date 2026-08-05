# SoC DTS Details

## Memory Blocks
Multiple reserved memory regions defined for various subsystems:

- MCUPM, SSPM, gz-log, unmap, platform_mtksmmu_protpgd, gz, me_cmdq_reserved: Reserved for firmware and secure processing
- framebuffer: Display framebuffer memory
- atf-log-reserved, log_store: Logging buffers
- emi_mbist_buf, dramc-rk1, dramc-rk0: DRAM testing and calibration
- aee_lk, BL31-reserved, minirdump, pstore, aee_debug_kinfo: Debug and crash dump memory

## CMA (Contiguous Memory Allocator) Pools
- ssmr-reserved-cma_memory: Shared DMA pool for SSMR
- ssheap-reserved-cma_memory: Shared heap memory
- ccci-dpmaif-cache-memory: CCCI DPMAIF cache
- ccci-dpmaif-nocache-memory: CCCI DPMAIF non-cacheable memory
- cmdq-resv-memory: Command queue reserved memory

## Memory Features
memory-ssmr-features: SSMR feature configuration
ssmr, ssheap: Memory subsystem configurations
drm-wv: DRM Widevine support
mtee-svp: Secure video path support
