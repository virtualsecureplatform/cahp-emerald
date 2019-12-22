hnit:
        js main

fib_init:
        li s0, 0
        li s1, 1
        li s2, 50
        jr ra

fib_main:
        add2 s0 ,s1
        blt s2, s1, fib_return
        add2 s1, s0
        bleu s2, s1, fib_return
        js fib_main
fib_return:
        jr ra
main:
        jsal fib_init
        jsal fib_main
        mov x8, s0
        nop
        js 0
