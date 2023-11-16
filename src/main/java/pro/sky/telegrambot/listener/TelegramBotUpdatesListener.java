package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.ScheduleTask;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final String START = "/start";
    private static final Pattern PATTERN = Pattern.compile(
            "([0-3][0-9]\\.[0-1][0-9]\\.[2-9][0-9]{3}" + //date
                    ".[0-2][0-9]:[0-5][0-9])" +          //time
                    "(.+)"                               //text
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ScheduleTask scheduleTask;

    private final TelegramBot telegramBot;

    public TelegramBotUpdatesListener(ScheduleTask scheduleTask, TelegramBot telegramBot) {
        this.scheduleTask = scheduleTask;
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                        logger.info("Processing update: {}", update);
                        // Process your updates here
                        if (update.message() == null) {
                            logger.info("Null message was sent");
                            return;
                        }

                        Long chatId = update.message().chat().id();
                        String messageText = update.message().text();

                        if (messageText.equals(START)) {
                            String userName;
                            if (update.message().chat().username() == null) {
                                userName = update.message().chat().lastName()
                                        + " "
                                        + update.message().chat().firstName();
                            } else userName = update.message().chat().username();
                            startMessage(chatId, userName);
                            return;
                        }

                        Matcher matcher = PATTERN.matcher(messageText);

                        if (matcher.matches()) {
                            String date = matcher.group(1);
                            LocalDateTime taskTime = LocalDateTime.parse(date, DATE_TIME_FORMATTER);//exception java.time.format.DateTimeParseException

                            if (taskTime.isBefore(LocalDateTime.now())) {
                                sendMessage(chatId, "Попробуй еще раз ввести дату и время правильно." +
                                        "\nНапример так: " +
                                        "\n31.12.2023 18:00 КАНУН НОВОГО ГОДА - обзвонить всех родных и друзей!");
                                logger.info("Date is before now {}", chatId);
                                return;
                            }

                            String notification = matcher.group(2);
                            scheduleTask.saveNotificationToDb(chatId, notification, taskTime);
                            logger.info("Notification was saved into DB {}", chatId);
                            sendMessage(chatId, "УРА!!! напоминание создалось успешно!!!\uD83D\uDC4D" +
                                    "\nВ указанный ДЕНЬ и ВРЕМЯ я обязательно напомню!" +
                                    "\n" +
                                    "\nможет создадим еще  \uD83D\uDC49 \"напоминалки\"?  \uD83E\uDD79");
                        }
                        if (!matcher.matches()) {
                            sendMessage(chatId, "Не удалось создать напоминание \uD83D\uDE22\n(введены некорректные данные)");
                        }
                    }
            );
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void startMessage(Long chatId, String userName) {
        logger.info("Was invoked method ___ startMessage {}", chatId);
        String responseMessage = "Привет, " + userName + "! \uD83D\uDC4B " +
                "\nЕсть планы на определенный день и время? " +
                "\nМогу помочь напомнить о самых важных!" +
                "\nДля этого введи число и время в формате: " +
                "\ndd.MM.yyyy HH:mm " +
                " \uD83D\uDC49ОПИСАНИЕ НАПОМИНАНИЯ\uD83D\uDC48";
        sendMessage(chatId, responseMessage);
    }

    private void sendMessage(Long chatId, String sendingMessage) {
        logger.info("Was invoked method ___ sendMessage");
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), sendingMessage);
        telegramBot.execute(sendMessage);
    }
}
