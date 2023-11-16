package pro.sky.telegrambot.service;

import java.time.LocalDateTime;

public interface ScheduleTask {
    void saveNotificationToDb(long chatId, String notification, LocalDateTime date);
    void findNotificationFromDb();

}
