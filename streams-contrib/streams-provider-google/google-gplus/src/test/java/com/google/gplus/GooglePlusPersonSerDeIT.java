/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.gplus;

import org.apache.streams.jackson.StreamsJacksonMapper;
import org.apache.streams.pojo.json.Activity;
import org.apache.streams.pojo.json.ActivityObject;
import org.apache.streams.pojo.json.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.api.services.plus.model.Person;
import com.google.gplus.serializer.util.GPlusPersonDeserializer;
import com.google.gplus.serializer.util.GooglePlusActivityUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests conversion of gplus inputs to Activity.
 */
public class GooglePlusPersonSerDeIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(GooglePlusPersonSerDeIT.class);
  private ObjectMapper objectMapper;
  private GooglePlusActivityUtil googlePlusActivityUtil;

  /**
   * setup.
   */
  @BeforeClass
  public void setup() {
    objectMapper = StreamsJacksonMapper.getInstance();
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addDeserializer(Person.class, new GPlusPersonDeserializer());
    objectMapper.registerModule(simpleModule);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    googlePlusActivityUtil = new GooglePlusActivityUtil();
  }

  @Test
  public void testPersonObjects() {
    InputStream is = GooglePlusPersonSerDeIT.class.getResourceAsStream("/google_plus_person_jsons.txt");
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    try {
      while (br.ready()) {
        String line = br.readLine();
        if (!StringUtils.isEmpty(line)) {
          LOGGER.info("raw: {}", line);
          Activity activity = new Activity();

          Person person = objectMapper.readValue(line, Person.class);

          GooglePlusActivityUtil.updateActivity(person, activity);
          LOGGER.info("activity: {}", activity);

          assertNotNull(activity);
          assertTrue (activity.getId().contains("id:googleplus:update"));
          assertEquals(activity.getVerb(), "update");

          Provider provider = activity.getProvider();
          assertEquals(provider.getId(), "id:providers:googleplus");
          assertEquals(provider.getDisplayName(), "GooglePlus");

          ActivityObject actor = activity.getActor();
          assertNotNull(actor.getImage());
          assertTrue (actor.getId().contains("id:googleplus:"));
          assertNotNull(actor.getUrl());

        }
      }
    } catch (Exception ex) {
      LOGGER.error("Exception while testing serializability: {}", ex);
    }
  }
}