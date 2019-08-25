# YARC - Yet Another RISC-V Chip
This is a project where I will implement a RISC-V SoC in [Chisel](https://github.com/freechipsproject/chisel3).
This project is only meant to be the reflection of the history of my personal learning progress.
## Current State of the `master` branch
The master branch contains a 5-stage RISC-V core with RV32I instructions implemented, fully interlocked, no data forwarding (bypassing). Using a dual-port async-read sync-write scratchpad memory.
* Only word-aligned word load/store (`LW`, `SW`) instruction allowed.
* Write to `0xFFF8` to print, `0xFFFC` to terminate simulation.
* Fill mem.txt with content dumped by yars.
* Suppose to work with [yqszxx/yars@`e69bab0`](https://github.com/yqszxx/yars/commit/e69bab0)
* Tested againt [yqszxx/yarc-testsw@`980b528`](https://github.com/yqszxx/yarc-testsw/commit/980b528)

