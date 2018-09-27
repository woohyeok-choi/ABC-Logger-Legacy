package kaist.iclab.abc.background

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import androidx.work.Worker
import kaist.iclab.abc.background.collector.NotificationCollector
import kaist.iclab.abc.common.util.WorkerUtils
import kaist.iclab.abc.data.entities.ParticipationEntity

class CollectorWorker : Worker() {
    override fun doWork(): Result {
        return try {

            val entity = ParticipationEntity.getParticipatedExperimentFromLocal()
            if (!entity.checkValidTimeRange(System.currentTimeMillis())) throw Exception()

            startServices(applicationContext)
            Result.SUCCESS
        } catch (e: Exception) {
            stopServices(applicationContext)
            Result.FAILURE
        }
    }

    companion object {
        fun start(isForced: Boolean = false) {
            WorkerUtils.startPeriodicWorkerAsync<CollectorWorker>(1000 * 60 * 15, isForced)
        }

        fun stop(context: Context) {
            WorkerUtils.stopPeriodicWorkerAsync<CollectorWorker>()
            stopServices(context)
        }

        private fun startServices(context: Context) {
            try { ContextCompat.startForegroundService(context, Intent(context, CollectorService::class.java)) } catch (e: Exception) { }
            try { context.startService(Intent(context, NotificationCollector::class.java)) } catch (e: Exception) { }
        }

        private fun stopServices(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, CollectorService::class.java)
                    .putExtra(CollectorService.EXTRA_STOP_SERVICE, true))
            } catch (e: Exception) { }
            try { context.stopService(Intent(context, NotificationCollector::class.java)) } catch (e: Exception) { }
        }
    }
}