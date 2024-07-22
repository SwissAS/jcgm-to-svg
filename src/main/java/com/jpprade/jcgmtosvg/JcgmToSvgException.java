package com.jpprade.jcgmtosvg;

import java.text.MessageFormat;

public class JcgmToSvgException extends RuntimeException {
	public JcgmToSvgException() {
	}
	
	public JcgmToSvgException(String message) {
		super(message);
	}
	
	public JcgmToSvgException(Throwable cause) {
		super(cause);
	}
	
	public JcgmToSvgException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public JcgmToSvgException(String message, Object... params) {
		this(MessageFormat.format(message, params));
	}
	
}
