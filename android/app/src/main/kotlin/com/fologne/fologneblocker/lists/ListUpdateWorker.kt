package com.fologne.fologneblocker.lists

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ListUpdateWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result =
        if (FilterRepository.get(applicationContext).refresh()) Result.success() else Result.retry()

    internal companion object {
        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<ListUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "fologneblocker-list-update", ExistingPeriodicWorkPolicy.KEEP, work,
            )
        }
    }
}
