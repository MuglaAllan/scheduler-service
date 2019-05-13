package je.dvs.echo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    CalendarServiceImpl calendarservice;

//    @Scheduled(cron="* 5 * * * *")
    @Scheduled(cron="* * 5 * * *")//every 30 seconds for testing
    public void scheduleTaskWithFixedRate() throws Exception {

        logger.info("*** Start clean Cron job at   ::  - {}", dateTimeFormatter.format(LocalDateTime.now()) );
        LocalDate dateTime = calendarservice.getNextBusinessDay(LocalDate.now().minusDays(1), 1);
        calendarservice.removeLockedSlotsFromDate(dateTime);
    }
}
