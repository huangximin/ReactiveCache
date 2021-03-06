/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivecache.expiration;

import io.reactivecache.Jolyglot$;
import io.reactivecache.Mock;
import io.reactivecache.ReactiveCache;
import io.reactivecache.common.BaseTestEvictingTask;
import io.rx_cache.internal.cache.EvictExpirableRecordsPersistence;
import java.util.List;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import rx.observers.TestSubscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class EvictExpirableRecordsTest extends BaseTestEvictingTask {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ReactiveCache reactiveCache;
  private int maxMgPersistenceCache = 7;

  @Before public void setUp() {
    reactiveCache = new ReactiveCache.Builder()
        .diskCacheSize(maxMgPersistenceCache)
        .using(temporaryFolder.getRoot(), Jolyglot$.newInstance());
  }

  @Test public void When_Expirable_Records_Evict() {
    assertThat(getSizeMB(temporaryFolder.getRoot()), is(0));

    for (int i = 0; i < 50; i++) {
      waitTime(50);
      TestSubscriber<List<Mock>> subscriber = new TestSubscriber<>();
      String key = i + "";

      createObservableMocks()
          .compose(reactiveCache.<List<Mock>>provider()
              .withKey(key)
              .readWithLoader()
          )
          .subscribe(subscriber);

      subscriber.awaitTerminalEvent();
    }

    waitTime(5000);
    int expectedStoredMB = (int) (maxMgPersistenceCache * EvictExpirableRecordsPersistence.PERCENTAGE_MEMORY_STORED_TO_STOP);
    assertThat(getSizeMB(temporaryFolder.getRoot()), is(expectedStoredMB));
  }

  @Test public void When_No_Expirable_Records_Do_Not_Evict() {
    assertThat(getSizeMB(temporaryFolder.getRoot()), is(0));

    for (int i = 0; i < 50; i++) {
      waitTime(50);
      TestSubscriber<List<Mock>> subscriber = new TestSubscriber<>();
      String key = i + "";
      createObservableMocks()
          .compose(reactiveCache.<List<Mock>>provider()
              .expirable(false)
              .withKey(key)
              .readWithLoader())
          .subscribe(subscriber);
      subscriber.awaitTerminalEvent();
    }

    waitTime(1000);
    assertThat(getSizeMB(temporaryFolder.getRoot()), is(maxMgPersistenceCache));
  }
}
