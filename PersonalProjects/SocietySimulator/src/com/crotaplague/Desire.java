package com.crotaplague;

public enum Desire{
    REPRESENTATIVE, CHAMBERLAIN;
    public static Desire valueOf(int i){
        return switch (i) {
            case 0 -> REPRESENTATIVE;
            case 1 -> CHAMBERLAIN;
            default -> REPRESENTATIVE;
        };
    }
}
