package pro.sky.telegrambot.service.imp;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.ScheduleTask;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduleTaskImp implements ScheduleTask {
    private final NotificationTaskRepository repository;
    private final TelegramBot telegramBot;

    public ScheduleTaskImp(NotificationTaskRepository repository, TelegramBot telegramBot) {
        this.repository = repository;
        this.telegramBot = telegramBot;
    }

    @Override
    public void saveNotificationToDb(long chatId, String notification, LocalDateTime date) {
        LocalDateTime taskTime = date.truncatedTo(ChronoUnit.MINUTES);
        NotificationTask notificationTask = new NotificationTask(chatId, notification, taskTime);
        repository.save(notificationTask);
    }

    @Scheduled(cron = "0 0/1 * * * *")
    @Override
    public void findNotificationFromDb() {
        LocalDateTime dateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<NotificationTask> tasks = repository.findByTaskTime(dateTime);
        tasks.forEach(t -> {
            SendMessage sendNotification = new SendMessage(t.getChatId(),"\"позволь\" напомнить, что НАДО:  \uD83D\uDC49 " + t.getNotificationTask().toUpperCase());
            telegramBot.execute(sendNotification);
        });
    }
}
