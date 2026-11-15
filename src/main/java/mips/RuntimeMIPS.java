package mips;

/** Runtime MARS32: referencias de una palabra y manejadores enlazados en pila. */
public final class RuntimeMIPS {
    private RuntimeMIPS() { }
    public static String datos() {
        return """
                .align 2
                __rt_handler: .word 0
                __rt_exception: .word 0
                __rt_div_msg: .asciiz "division por cero"
                __rt_index_msg: .asciiz "indice fuera de rango"
                __rt_null_msg: .asciiz "referencia nula"
                """;
    }
    public static String texto() {
        StringBuilder s = new StringBuilder("""
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
                """);
        for (int i = 0; i < 8; i++) s.append("    lw $s").append(i).append(", ").append(20 + i * 4).append("($v1)\n");
        return s + """
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
                """;
    }
}
