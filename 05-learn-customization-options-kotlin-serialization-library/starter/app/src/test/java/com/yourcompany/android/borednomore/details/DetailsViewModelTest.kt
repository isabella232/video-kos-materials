/*
 * Copyright (c) 2022 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yourcompany.android.borednomore.details

import com.yourcompany.android.borednomore.BoredActivityRepository
import com.yourcompany.android.borednomore.data.FakeDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class DetailsViewModelTest {
  private lateinit var executor: ExecutorService

  @Before
  fun setup() {
    executor = Executors.newSingleThreadExecutor()
    Dispatchers.setMain(executor.asCoroutineDispatcher())
  }

  @After
  fun cleanup() {
    executor.shutdown()
    Dispatchers.resetMain()
  }

  @Test
  fun `should start with Uninitialized state`() {
    val dataSource = FakeDataSource()
    val repository = BoredActivityRepository(dataSource)
    val viewModel = DetailsViewModel(repository)

    assertEquals(DetailsState.Uninitialized, viewModel.state.value)
  }

  @Test
  fun `should emit Loading when fetching data`() = runBlocking {
    val dataSource = FakeDataSource()
    val repository = BoredActivityRepository(dataSource)
    val viewModel = DetailsViewModel(repository)

    val vmTask = async(start = CoroutineStart.LAZY) { viewModel.getDetails("<mock-key>") }
    val stateTask = async { viewModel.state.take(2).toList() }

    vmTask.await()
    val states = stateTask.await()
    val expectedState = states.last()

    assertTrue(expectedState is DetailsState.Loading)
  }

  @Test
  fun `should emit Error when data fetch fails`() = runBlocking {
    val dataSource = FakeDataSource()
    val repository = BoredActivityRepository(dataSource)
    val viewModel = DetailsViewModel(repository)

    val vmTask = async(start = CoroutineStart.LAZY) { viewModel.getDetails("invalid-key") }
    val stateTask = async { viewModel.state.take(3).toList() }

    vmTask.await()
    val (_, _, thirdState) = stateTask.await()

    assertTrue(thirdState is DetailsState.Error)
  }

  @Test
  fun `should emit Success when data fetch is successful`() = runBlocking {
    val dataSource = FakeDataSource()
    val repository = BoredActivityRepository(dataSource)
    val viewModel = DetailsViewModel(repository)
    val (activity) = dataSource.getMany(Int.MAX_VALUE).unsafeUnwrap()

    val vmTask = async(start = CoroutineStart.LAZY) { viewModel.getDetails(activity.key) }
    val stateTask = async { viewModel.state.take(3).toList() }

    vmTask.await()
    val (_, _, thirdState) = stateTask.await()
    assertTrue(thirdState is DetailsState.Success)
  }
}