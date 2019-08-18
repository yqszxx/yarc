# YARC - Yet Another RISC-V Chip `bn-in-fn`
5-stage RISC-V core with no branch instruction, no interlock (data dependency check), no data forwarding (bypassing). Using a dual-port async-read sync-write scratchpad memory.
* Only word-aligned word load/store instruction allowed.
* Write to `0xFFF8` to print, `0xFFFC` to terminate simulation.
* Fill mem.txt with content dumped by yars.
* Suppose to work with yqszxx/yars@e69bab04bf621e696da934e97e78e7f8da74342f
