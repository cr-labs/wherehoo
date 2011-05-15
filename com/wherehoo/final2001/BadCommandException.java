package com.wherehoo.final2001;

public class BadCommandException extends Exception {
	private static final long serialVersionUID = 1L;

	public BadCommandException(){}
    public BadCommandException(String s) {
        super(s);
    }
}
