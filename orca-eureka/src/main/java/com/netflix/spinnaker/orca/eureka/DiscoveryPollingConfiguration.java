/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.eureka;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.spinnaker.orca.restart.InstanceStatusProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.management.ManagementFactory;
import java.util.Optional;

@Configuration
public class DiscoveryPollingConfiguration {

  @Configuration
  @ConditionalOnMissingBean(DiscoveryClient.class)
  public static class NoDiscoveryConfiguration {
    @Autowired
    ApplicationEventPublisher publisher;

    @Value("${spring.application.name:orca}")
    String appName;

    @Bean
    public ApplicationListener<ContextRefreshedEvent> discoveryStatusPoller() {
      return new NoDiscoveryApplicationStatusPublisher(publisher);
    }

    @Bean
    public String currentInstanceId() {
      return ManagementFactory.getRuntimeMXBean().getName();
    }

    @Bean
    public InstanceStatusProvider instanceStatusProvider(String currentInstanceId) {
      return (String app, String instanceId) -> appName.equals(app) && currentInstanceId.equals(instanceId);
    }
  }

  @Configuration
  @ConditionalOnBean(DiscoveryClient.class)
  public static class DiscoveryConfiguration {
    @Bean
    public String currentInstanceId(InstanceInfo instanceInfo) {
      return instanceInfo.getInstanceId();
    }

    @Bean
    public InstanceStatusProvider instanceStatusProvider(DiscoveryClient discoveryClient) {
      return (String app, String instanceId) -> Optional.ofNullable(discoveryClient.getApplication(app))
        .map(a -> a.getByInstanceId(instanceId))
        .map(InstanceInfo::getStatus)
        .orElse(InstanceInfo.InstanceStatus.UNKNOWN)  == InstanceInfo.InstanceStatus.UP;
    }
  }
}
