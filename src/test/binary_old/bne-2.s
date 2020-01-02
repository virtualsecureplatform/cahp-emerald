li x1, 2
li x2, 2
bne x1, x2, L2
L1:
li x8, 0x2A
js L1
L2:
li x8, 1
js L2
