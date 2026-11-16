.data
g_cero: .word 0
g_x: .word 0
.align 2
__rt_handler: .word 0
__rt_exception: .word 0
__rt_div_msg: .asciiz "division por cero"
__rt_index_msg: .asciiz "indice fuera de rango"
__rt_null_msg: .asciiz "referencia nula"

.text
.globl main
main:
    addiu $sp, $sp, -16
    sw $ra, 12($sp)
    sw $fp, 8($sp)
    addiu $fp, $sp, 16

L0:
    li $t0, 7
    li $t0, 7
    move $t0, $t0
    addu $t1, $t0, $zero
    move $a0, $t1
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    subu $t1, $t0, $zero
    move $a0, $t1
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    li $t2, 0
    move $a0, $t2
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    move $t1, $t0
    move $a0, $t1
    sw $t2, g_cero
    sw $t1, -12($fp)
    sw $t0, g_x
    jal func_identidad
    move $t0, $v0
    move $a0, $t0
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    sw $t0, -16($fp)

main_epilogo:
    li $v0, 10
    syscall

func_identidad:
    addiu $sp, $sp, -16
    sw $ra, 12($sp)
    sw $fp, 8($sp)
    addiu $fp, $sp, 16
    sw $s0, -16($fp)
    sw $a0, -12($fp)
    lw $s0, -12($fp)
    move $v0, $s0
    j func_identidad_epilogo

func_identidad_epilogo:
    lw $s0, -16($fp)
    lw $ra, -4($fp)
    move $sp, $fp
    lw $fp, -8($fp)
    jr $ra

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
