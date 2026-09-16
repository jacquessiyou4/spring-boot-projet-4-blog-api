package com.kfokam48.exception;

/**
 * Levée lorsqu'un article ou un commentaire demandé n'existe pas → HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
