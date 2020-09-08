package com;

import java.util.Random;

public class ss {
    public static void main(String[] args) {
        Random r = new Random();
        int n2 = r.nextInt(10);
        int n3 = Math.abs(r.nextInt() % 10);
        System.out.println("n2:"+n2);
        System.out.println("n3:"+n3);
    }
    }


