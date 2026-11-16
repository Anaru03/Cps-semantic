.data
g__global_0_fila: .word 0
g__global_1_v: .word 0
g_m: .word 0
g_suma: .word 0
g_xs: .word 0
.align 2
__rt_handler: .word 0
__rt_exception: .word 0
__rt_div_msg: .asciiz "division por cero"
__rt_index_msg: .asciiz "indice fuera de rango"
__rt_null_msg: .asciiz "referencia nula"

.text
.globl main
main:
    addiu $sp, $sp, -40
    sw $ra, 36($sp)
    sw $fp, 32($sp)
    addiu $fp, $sp, 40
    li $a0, 16
    li $v0, 9
    syscall
    li $v1, 3
    sw $v1, 0($v0)
    addiu $a1, $v0, 4
    li $a2, 12

__rt_zero_0:
    beq $a2, $zero, __rt_zero_0_fin
    sw $zero, 0($a1)
    addiu $a1, $a1, 4
    addiu $a2, $a2, -4
    j __rt_zero_0

__rt_zero_0_fin:
    move $t0, $v0
    sw $t0, -12($fp)
    lw $t0, -12($fp)
    beq $t0, $zero, __rt_null
    bltz $zero, __rt_index
    lw $v1, 0($t0)
    bge $zero, $v1, __rt_index
    sll $v1, $zero, 2
    addu $v1, $t0, $v1
    li $t1, 10
    sw $t1, 4($v1)
    lw $t0, -12($fp)
    li $t1, 1
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    li $t2, 20
    sw $t2, 4($v1)
    lw $t0, -12($fp)
    li $t1, 2
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    li $t2, 30
    sw $t2, 4($v1)
    move $t1, $t0
    sw $t1, g_xs
    lw $t0, g_xs
    beq $t0, $zero, __rt_null
    bltz $zero, __rt_index
    lw $v1, 0($t0)
    bge $zero, $v1, __rt_index
    sll $v1, $zero, 2
    addu $v1, $t0, $v1
    lw $t1, 4($v1)
    sw $t1, -12($fp)
    lw $t0, g_xs
    li $t1, 2
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    lw $t2, 4($v1)
    lw $t1, -12($fp)
    addu $t3, $t1, $t2
    sw $t2, -16($fp)
    sw $t3, -20($fp)
    lw $t0, g_xs
    li $t1, 1
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    lw $t2, -20($fp)
    sw $t2, 4($v1)
    li $a0, 12
    li $v0, 9
    syscall
    li $v1, 2
    sw $v1, 0($v0)
    addiu $a1, $v0, 4
    li $a2, 8

__rt_zero_1:
    beq $a2, $zero, __rt_zero_1_fin
    sw $zero, 0($a1)
    addiu $a1, $a1, 4
    addiu $a2, $a2, -4
    j __rt_zero_1

__rt_zero_1_fin:
    move $t0, $v0
    sw $t0, -12($fp)
    li $a0, 12
    li $v0, 9
    syscall
    li $v1, 2
    sw $v1, 0($v0)
    addiu $a1, $v0, 4
    li $a2, 8

__rt_zero_2:
    beq $a2, $zero, __rt_zero_2_fin
    sw $zero, 0($a1)
    addiu $a1, $a1, 4
    addiu $a2, $a2, -4
    j __rt_zero_2

__rt_zero_2_fin:
    move $t0, $v0
    sw $t0, -16($fp)
    lw $t0, -16($fp)
    beq $t0, $zero, __rt_null
    bltz $zero, __rt_index
    lw $v1, 0($t0)
    bge $zero, $v1, __rt_index
    sll $v1, $zero, 2
    addu $v1, $t0, $v1
    li $t1, 1
    sw $t1, 4($v1)
    lw $t0, -16($fp)
    li $t1, 1
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    li $t2, 2
    sw $t2, 4($v1)
    lw $t0, -12($fp)
    beq $t0, $zero, __rt_null
    bltz $zero, __rt_index
    lw $v1, 0($t0)
    bge $zero, $v1, __rt_index
    sll $v1, $zero, 2
    addu $v1, $t0, $v1
    lw $t1, -16($fp)
    sw $t1, 4($v1)
    li $a0, 12
    li $v0, 9
    syscall
    li $v1, 2
    sw $v1, 0($v0)
    addiu $a1, $v0, 4
    li $a2, 8

__rt_zero_3:
    beq $a2, $zero, __rt_zero_3_fin
    sw $zero, 0($a1)
    addiu $a1, $a1, 4
    addiu $a2, $a2, -4
    j __rt_zero_3

__rt_zero_3_fin:
    move $t0, $v0
    sw $t0, -20($fp)
    lw $t0, -20($fp)
    beq $t0, $zero, __rt_null
    bltz $zero, __rt_index
    lw $v1, 0($t0)
    bge $zero, $v1, __rt_index
    sll $v1, $zero, 2
    addu $v1, $t0, $v1
    li $t1, 3
    sw $t1, 4($v1)
    lw $t0, -20($fp)
    li $t1, 1
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    li $t2, 4
    sw $t2, 4($v1)
    lw $t0, -12($fp)
    li $t1, 1
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    lw $t2, -20($fp)
    sw $t2, 4($v1)
    move $t1, $t0
    move $t3, $zero
    move $t4, $t1
    sw $t1, g_m
    sw $t3, g_suma
    sw $t4, -16($fp)
    lw $t0, -16($fp)
    beq $t0, $zero, __rt_null
    lw $t1, 0($t0)
    move $t2, $zero
    sw $t2, -12($fp)
    sw $t1, -20($fp)

L0:
    lw $t0, -12($fp)
    lw $t1, -20($fp)
    slt $t2, $t0, $t1
    sw $t2, -24($fp)
    bne $t2, $zero, L1
    j L3

L1:
    lw $t0, -16($fp)
    lw $t1, -12($fp)
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    lw $t2, 4($v1)
    move $t3, $t2
    sw $t2, g__global_0_fila
    sw $t3, -24($fp)
    lw $t0, -24($fp)
    beq $t0, $zero, __rt_null
    lw $t1, 0($t0)
    move $t2, $zero
    sw $t1, -28($fp)
    sw $t2, -32($fp)

L4:
    lw $t0, -32($fp)
    lw $t1, -28($fp)
    slt $t2, $t0, $t1
    sw $t2, -36($fp)
    bne $t2, $zero, L5
    j L7

L5:
    lw $t0, -24($fp)
    lw $t1, -32($fp)
    beq $t0, $zero, __rt_null
    bltz $t1, __rt_index
    lw $v1, 0($t0)
    bge $t1, $v1, __rt_index
    sll $v1, $t1, 2
    addu $v1, $t0, $v1
    lw $t2, 4($v1)
    lw $t3, g_suma
    addu $t4, $t3, $t2
    move $t3, $t4
    sw $t2, g__global_1_v
    sw $t3, g_suma
    sw $t4, -36($fp)

L6:
    lw $t0, -32($fp)
    li $t1, 1
    addu $t2, $t0, $t1
    move $t0, $t2
    sw $t0, -32($fp)
    sw $t2, -36($fp)
    j L4

L7:

L2:
    lw $t0, -12($fp)
    li $t1, 1
    addu $t2, $t0, $t1
    move $t0, $t2
    sw $t0, -12($fp)
    sw $t2, -36($fp)
    j L0

L3:
    lw $t0, g_xs
    beq $t0, $zero, __rt_null
    lw $t1, 0($t0)
    lw $t2, g_suma
    addu $t3, $t2, $t1
    move $a0, $t3
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    sw $t1, -24($fp)
    sw $t3, -28($fp)

main_epilogo:
    li $v0, 10
    syscall


__rt_div:
    la $a0, __rt_div_msg
    j __rt_throw
__rt_index:
    la $a0, __rt_index_msg
    j __rt_throw
__rt_null:
    la $a0, __rt_null_msg
__rt_throw:
    sw $a0, __rt_exception
    lw $v1, __rt_handler
    beq $v1, $zero, __rt_uncaught
    lw $a1, 0($v1)
    sw $a1, __rt_handler
    lw $a2, 4($v1)
    lw $sp, 8($v1)
    lw $fp, 12($v1)
    lw $ra, 16($v1)
    lw $s0, 20($v1)
    lw $s1, 24($v1)
    lw $s2, 28($v1)
    lw $s3, 32($v1)
    lw $s4, 36($v1)
    lw $s5, 40($v1)
    lw $s6, 44($v1)
    lw $s7, 48($v1)
    jr $a2
__rt_uncaught:
    li $v0, 4
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    li $a0, 1
    li $v0, 17
    syscall
