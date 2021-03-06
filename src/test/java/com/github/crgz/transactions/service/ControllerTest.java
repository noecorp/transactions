/*******************************************************************************
 * Copyright (C) 2017, Conrado M. Rodriguez
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.github.crgz.transactions.service;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.github.crgz.transactions.model.Event;
import com.github.crgz.transactions.model.Statistics;
import com.github.crgz.utils.EventStatisticsMapper;
import com.github.crgz.utils.WorkbookData;

public class ControllerTest
{
	private static final int TEST_DATA_ABSOLUTE_WINDOW = 3;
	private static final Logger logger = LogManager.getLogger(ControllerTest.class.getName());

	private Collection<Object[]> data;
	private Controller controller;
	private Duration period;

	@Before
	public void setUp() throws Exception
	{
		this.data = new WorkbookData.Builder("src/test/resources/sample.xls").sheet(0).build().collection();
		Duration timeWindow = Duration.ofSeconds(TEST_DATA_ABSOLUTE_WINDOW);
		period = timeWindow.dividedBy(TEST_DATA_ABSOLUTE_WINDOW);
		logger.info("Defining a test time window of {} seconds and a request period of {} seconds", timeWindow.getSeconds(), period);
		
		this.controller = new Controller(timeWindow);
	}

	@Test
	public void testStatisticsController() throws Exception
	{
		logger.info("Sending events every {} {}", period.getSeconds(), TimeUnit.SECONDS);

		this.data.stream().map(EventStatisticsMapper::map).forEach(data ->
		{
			final Event event = getData(Instant.now(), data);
			final Statistics expected = data.getValue();
			
			Duration transactionsEta = controller.accept(event);
			logger.info("Sent: {}. The expected time for stats is {}", event, transactionsEta.toMillis());
			try
			{
				TimeUnit.MILLISECONDS.sleep(period.dividedBy(2).toMillis());
				Statistics actual = controller.get();
				TimeUnit.MILLISECONDS.sleep(period.dividedBy(2).toMillis());
				logger.info("Expected: {} Received: {}", expected, actual);
				assertEquals(expected, actual);
			}
			catch (InterruptedException e)
			{
				logger.error(e.getLocalizedMessage(), e);
			}
		});
	}
	
	private Event getData(final Instant now, SimpleEntry<Event, Statistics> data)
	{
		return new Event(now.plusNanos(data.getKey().getInstant().getEpochSecond()), Double.valueOf(data.getKey().getAmount()).intValue() );
	}
}
