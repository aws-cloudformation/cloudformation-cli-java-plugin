package com.aws.cfn.cron;

import org.junit.Assert;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CronHelperTests {

    @Test
    public void testGenerateOneTimeCronExpression_Simple() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-10-30 13:40:23", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        Assert.assertEquals(
            "cron(45 13 30 10 ? 2018)",
            CronHelper.generateOneTimeCronExpression(5, mockClockInASock)
        );
    }

    @Test
    public void testGenerateOneTimeCronExpression_DayBreak() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-10-30 23:59:01", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        Assert.assertEquals(
            "cron(2 0 31 10 ? 2018)",
            CronHelper.generateOneTimeCronExpression(3, mockClockInASock)
        );
    }

    @Test
    public void testGenerateOneTimeCronExpression_YearBreak() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-12-31 23:56:59", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        Assert.assertEquals(
            "cron(1 0 1 1 ? 2019)",
            CronHelper.generateOneTimeCronExpression(5, mockClockInASock)
        );
    }
}

