package dev.coinbot.mobile;

import java.io.IOException;

/** Safe user-facing reason. Never includes paths, source text, or credentials. */
final class ImageGenerationException extends IOException {
    ImageGenerationException(String message){super(message);}
    ImageGenerationException(String message,Throwable cause){super(message,cause);}
}
