/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

public class CronHelperTest {

    @Test
    public void testGenerateOneTimeCronExpression_Simple() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-10-30 13:40:59", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        final CronHelper cronHelper = new CronHelper(mockClockInASock);

        assertThat(cronHelper.generateOneTimeCronExpression(5)).isEqualTo("cron(46 13 30 10 ? 2018)");
    }

    @Test
    public void testGenerateOneTimeCronExpression_DayBreak() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-10-30 23:59:01", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        final CronHelper cronHelper = new CronHelper(mockClockInASock);

        assertThat(cronHelper.generateOneTimeCronExpression(3)).isEqualTo("cron(3 0 31 10 ? 2018)");
    }

    @Test
    public void testGenerateOneTimeCronExpression_YearBreak() {

        final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final LocalDateTime dateTime = LocalDateTime.parse("2018-12-31 23:56:59", f);
        final Clock mockClockInASock = mock(Clock.class);
        when(mockClockInASock.instant()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));

        final CronHelper cronHelper = new CronHelper(mockClockInASock);

        assertThat(cronHelper.generateOneTimeCronExpression(5)).isEqualTo("cron(2 0 1 1 ? 2019)");
    }
}
