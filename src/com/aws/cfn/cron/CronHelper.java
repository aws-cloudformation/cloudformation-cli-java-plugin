package com.aws.cfn.cron;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CronHelper {

    /**
     * Creates a cron(..) expression for a single instance at Now+minutesFromNow
     * NOTE: CloudWatchEvents only support a 1minute granularity for re-invoke
     * Anything less should be handled inside the original handler request
     *
     * @param minutesFromNow The number of minutes from now for building the cron expression
     * @param clock Supply a Clock instance (used for testing)
     * @return A cron expression for use with CloudWatchEvents putRule(..) API
     * @apiNote Expression is of form cron(minutes, hours, day-of-month, month, day-of-year, year) where
     * day-of-year is not necessary when the day-of-month and month-of-year fields are supplied
     */
    public static String generateOneTimeCronExpression(final int minutesFromNow,
                                                       final Clock clock) {
        final Instant instant = Instant.now(clock).plusSeconds(60 * minutesFromNow);
        final OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);

        return DateTimeFormatter.ofPattern("'cron('m H d M ? u')'").format(odt);
    }

    /**
     * Creates a cron(..) expression for a single instance at Now+minutesFromNow
     * NOTE: CloudWatchEvents only support a 1minute granularity for re-invoke
     * Anything less should be handled inside the original handler request
     *
     * @param minutesFromNow The number of minutes from now for building the cron expression
     * @return A cron expression for use with CloudWatchEvents putRule(..) API
     * @apiNote Expression is of form cron(minutes, hours, day-of-month, month, day-of-year, year) where
     * day-of-year is not necessary when the day-of-month and month-of-year fields are supplied
     */
    public static String generateOneTimeCronExpression(final int minutesFromNow) {
        return  generateOneTimeCronExpression(minutesFromNow, Clock.systemUTC());
    }
}

