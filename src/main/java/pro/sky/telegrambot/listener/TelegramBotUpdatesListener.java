package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import pro.sky.telegrambot.entity.NotificationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.repositiry.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    @Autowired
    private NotificationTaskRepository notificationTaskRepository;
    @Autowired
    private TelegramBot telegramBot;
    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final String WELCOME_MSG_TEXT = "Для того, чтобы запланировать задачу отправьте ее в формате: \n **01.01.2023 12:00 Подготовиться к собеседованию ";
    private final String NOTIFICATION_TASK_PATTERN = "([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)";


    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                if (update.message() == null) {
                    return;
                }

                logger.info("Processing update: {}", update);

                String incomeMsgText = update.message().text();
                long userId = update.message().chat().id();
                if ("/start".equals(incomeMsgText)) {
                    sendMessage(userId, WELCOME_MSG_TEXT);
                } else {
                    Pattern pattern = Pattern.compile(NOTIFICATION_TASK_PATTERN);
                    Matcher matcher = pattern.matcher(incomeMsgText);
                    if (matcher.matches()) {
                        String date = matcher.group(1);
                        String message = matcher.group(3);
                        LocalDateTime notificationDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                        logger.info("Notification task message (date: {}, message: {})", notificationDateTime, message);
                        notificationTaskRepository.save(new NotificationTask(userId, message, notificationDateTime));
                        sendMessage(userId, "Задача запланированна!");
                    } else {
                        sendMessage(userId, "Некорректный формат сообщения!");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendMessage(Long userId, String messageText) {
        SendMessage message = new SendMessage(userId, messageText);
        SendResponse response = telegramBot.execute(message);
        if (!response.isOk()) {
            logger.warn("Message was not sent: {}, error code: {}", message, response.errorCode());
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotificationTasks() {

        Collection<NotificationTask> currentTasks = notificationTaskRepository.findAllTasksByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        Iterator<NotificationTask> iterator = currentTasks.iterator();
        while (iterator.hasNext()) {
            NotificationTask task = iterator.next();
            sendMessage(task.getUserId(), task.getMessage());
        }
    }

}
