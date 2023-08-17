package com.jari.backend;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        var backend = new Backend();
        backend.jarIt(new File(args[0]));

        if (!backend.isOk()) {
            backend.getErrors().forEach(System.out::println);
        }
    }
}
