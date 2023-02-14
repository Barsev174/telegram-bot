package pro.sky.telegrambot.repositiry;

import pro.sky.telegrambot.entity.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {
    @Query(value = "SELECT * FROM notification_task WHERE notification_date_time = :notification_date_time", nativeQuery = true)
    Collection<NotificationTask> findAllTasksByDateTime(
            @Param("notification_date_time") LocalDateTime notification_date_time
    );
}
