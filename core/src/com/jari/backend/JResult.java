package com.jari.backend;

import java.util.function.Function;
import java.util.function.Supplier;

public class JResult<Ok, Err> {
    public static <Ok, Err> JResult<Ok, Err> ok(Ok ok) {
        return new JResult<>(ok, null);
    }

    public static <Ok, Err> JResult<Ok, Err> err(Err err) {
        return new JResult<>(null, err);
    }

    public static <Ok, Err> JResult<Ok, Err> tryF(Supplier<Ok> okFun, Err errVal) {
        try {
            return JResult.ok(okFun.get());
        } catch (Exception e) {
            return JResult.err(errVal);
        }
    }

    public static <Ok, Err> JResult<Ok, Err> tryF(Supplier<Ok> okFun, Supplier<Err> errFun) {
        return tryF(okFun, errFun.get());
    }

    public static <Ok, Err> JResult<Ok, Err> tryF(Supplier<Ok> okFun, Function<Exception, JResult<Ok, Err>> fun) {
        try {
            return JResult.ok(okFun.get());
        } catch (Exception e) {
            return fun.apply(e);
        }
    }

    private Ok ok;
    private Err err;

    JResult(Ok ok, Err err) {
        if (ok == null) {
            this.ok = null;
            this.err = err;
        } else if (err == null) {
            this.ok = ok;
            this.err = null;
        }
    }

    public Ok unwrap() {
        if (isOk()) {
            return ok;
        }

        throw new RuntimeException("unwrap was called on an error value.");
    }

    public Err unwrapErr() {
        if (isOk()) {
            throw new RuntimeException("unwrapErr was called on an ok value.");
        }

        return err;
    }

    public Ok orElse(Supplier<Ok> supplierFun) {
        if (!isOk()) {
            return supplierFun.get();
        }

        return ok;
    }

    public Ok orThrow(Exception e) throws Exception {
        if (!isOk()) {
            throw e;
        }

        return ok;
    }

    public Ok orNull() {
        if (!isOk()) {
            return null;
        }

        return ok;
    }

    public boolean isOk() {
        return ok != null;
    }
}