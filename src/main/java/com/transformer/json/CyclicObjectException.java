package com.transformer.json;

public class CyclicObjectException extends RuntimeException {

    public CyclicObjectException( String str ) {
        super(str);
    }
}
