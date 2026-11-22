package com.seb.pedidos360.administrator.exception;

import org.apache.kafka.common.errors.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalHandlderException {

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<Map<String,Object>> handleNotFound(UnknownTopicOrPartitionException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "El tópico solicitado no existe: " + ex.getMessage());
    }

    @ExceptionHandler(TopicExistsException.class)
    public ResponseEntity<Map<String,Object>> handleConflict(TopicExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "El tópico ya existe: " + ex.getMessage());
    }

    @ExceptionHandler({InvalidPartitionsException.class, InvalidReplicationFactorException.class, InvalidConfigurationException.class})
    public ResponseEntity<Map<String,Object>> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String,Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }

    @ExceptionHandler(GroupIdNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGroupNotFound(GroupIdNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "El consumer group no existe: " + ex.getMessage());
    }

    @ExceptionHandler(GroupNotEmptyException.class)
    public ResponseEntity<Map<String, Object>> handleGroupNotEmpty(GroupNotEmptyException ex) {
        return buildResponse(HttpStatus.CONFLICT, "No se puede eliminar un consumer group con miembros activos.");
    }
}
