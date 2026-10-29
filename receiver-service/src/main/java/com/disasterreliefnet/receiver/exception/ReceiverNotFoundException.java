package com.disasterreliefnet.receiver.exception;

import com.disasterreliefnet.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ReceiverNotFoundException extends ResourceNotFoundException {
    public ReceiverNotFoundException(UUID receiverId) {
        super("Receiver", receiverId);
    }
}
