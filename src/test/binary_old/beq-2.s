li x1, 2
li x2, 1
beq x1, x2, L2
L1:
li x8, 0x2A
js L1
L2:
li x8, 0x1
js L2
