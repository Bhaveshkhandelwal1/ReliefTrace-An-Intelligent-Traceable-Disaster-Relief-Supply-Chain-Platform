package com.disasterreliefnet.notification.sender;

import com.disasterreliefnet.common.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component dispatching notifications across Email (SMTP), SMS (Twilio/Infobip), and Push (FCM/APNS).
 */
@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    public boolean send(String userId, NotificationChannel channel, String subject, String body) {
        log.info("Dispatching [{}] notification to user: {}, subject: '{}'", channel, userId, subject);
        // Simulates carrier/provider dispatch
        return true;
    }
}
