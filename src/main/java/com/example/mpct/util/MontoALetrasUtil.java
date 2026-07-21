package com.example.mpct.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class MontoALetrasUtil {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "};
    private static final String[] DECENAS = {"DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ",
            "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ", "VEINTE ", "TREINTA ", "CUARENTA ",
            "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "};
    private static final String[] CENTENAS = {"", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ",
            "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String convertir(BigDecimal numero) {
        String literal = "";
        String parteDecimal = "";
        
        long entero = numero.longValue();
        int decimal = numero.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();
        parteDecimal = String.format("%02d/100 SOLES", decimal);

        if (entero == 0) {
            literal = "CERO ";
        } else if (entero > 999999) {
            literal = getMillones(String.valueOf(entero));
        } else if (entero > 999) {
            literal = getMiles(String.valueOf(entero));
        } else if (entero > 99) {
            literal = getCentenas(String.valueOf(entero));
        } else if (entero > 9) {
            literal = getDecenas(String.valueOf(entero));
        } else {
            literal = getUnidades(String.valueOf(entero));
        }

        return (literal + "Y " + parteDecimal).replaceAll("  ", " ");
    }

    private static String getUnidades(String numero) {
        String num = numero.substring(numero.length() - 1);
        return UNIDADES[Integer.parseInt(num)];
    }

    private static String getDecenas(String numero) {
        int n = Integer.parseInt(numero.substring(numero.length() - 2));
        if (n < 10) return getUnidades(numero);
        if (n < 20) return DECENAS[n - 10];
        if (n == 20) return "VEINTE ";
        if (n > 20 && n < 30) return "VEINTI" + getUnidades(numero);
        int d = n / 10;
        int u = n % 10;
        if (u == 0) return DECENAS[d + 8];
        return DECENAS[d + 8] + "Y " + getUnidades(numero);
    }

    private static String getCentenas(String numero) {
        int c = Integer.parseInt(numero.substring(numero.length() - 3, numero.length() - 2));
        String dec = numero.substring(numero.length() - 2);
        if (c == 1 && dec.equals("00")) return "CIEN ";
        return CENTENAS[c] + getDecenas(dec);
    }

    private static String getMiles(String numero) {
        String c = numero.substring(numero.length() - 3);
        String m = numero.substring(0, numero.length() - 3);
        String n = "";
        if (Integer.parseInt(m) > 0) {
            n = getCentenas(m);
            if (n.equals("UN ")) n = "";
            n = n + "MIL ";
        }
        return n + getCentenas(c);
    }

    private static String getMillones(String numero) {
        String miles = numero.substring(numero.length() - 6);
        String millon = numero.substring(0, numero.length() - 6);
        String n = "";
        if (millon.length() > 1) {
            n = getCentenas(millon) + "MILLONES ";
        } else {
            if (millon.equals("1")) n = "UN MILLON ";
            else n = getUnidades(millon) + "MILLONES ";
        }
        return n + getMiles(miles);
    }
}
