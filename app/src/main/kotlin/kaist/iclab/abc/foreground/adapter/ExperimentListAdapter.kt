package kaist.iclab.abc.foreground.adapter

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.arch.paging.*
import android.support.v4.view.ViewCompat
import android.support.v7.util.DiffUtil
import android.view.View
import android.view.ViewGroup
import kaist.iclab.abc.common.type.LoadState
import kaist.iclab.abc.communication.GrpcApi
import kaist.iclab.abc.foreground.listener.BaseViewHolder
import kaist.iclab.abc.foreground.listener.OnRecyclerViewItemClickListener
import kaist.iclab.abc.foreground.view.ExperimentItemView
import kaist.iclab.abc.protos.ExperimentProtos
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class ExperimentListAdapter: PagedListAdapter<ExperimentProtos.ExperimentEssential, ExperimentListAdapter.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExperimentProtos.ExperimentEssential>() {
            override fun areItemsTheSame(oldItem: ExperimentProtos.ExperimentEssential, newItem: ExperimentProtos.ExperimentEssential): Boolean {
                return oldItem.basic.uuid == newItem.basic.uuid
            }

            override fun areContentsTheSame(oldItem: ExperimentProtos.ExperimentEssential, newItem: ExperimentProtos.ExperimentEssential): Boolean {
                return oldItem == newItem
            }
        }
        private val TAG = ExperimentListAdapter::class.java.simpleName
    }

    private var listener: OnRecyclerViewItemClickListener<ExperimentProtos.ExperimentEssential>? = null

    fun setOnItemClickListener(onItemClickListener: OnRecyclerViewItemClickListener<ExperimentProtos.ExperimentEssential>?) {
        listener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ExperimentItemView(parent.context)) { position, view ->
            listener?.onItemClick(position, getItem(position), view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if(item == null) {
            holder.clearView()
        } else {
            holder.bindView(item)
            ViewCompat.setTransitionName(holder.view, item.basic.uuid)
        }
    }

    class ExperimentDataSource : PageKeyedDataSource<Long, ExperimentProtos.ExperimentEssential>() {
        val loadState = MutableLiveData<LoadState>()
        val initialLoadState  = MutableLiveData<LoadState>()

        private var retryFunc: (() -> Any)? = null

        fun retry(executor: Executor) {
            val prevRetry = retryFunc
            retryFunc = null

            prevRetry?.let {
                executor.execute {
                    it.invoke()
                }
            }
        }

        override fun loadInitial(params: LoadInitialParams<Long>, callback: LoadInitialCallback<Long, ExperimentProtos.ExperimentEssential>) {
            initialLoadState.postValue(LoadState.LOADING)
            loadState.postValue(LoadState.LOADING)

            try {
                GrpcApi.listExperiments(limit = params.requestedLoadSize).let {
                    if(it.isEmpty()) {
                        callback.onResult(it, null, null)
                    } else {
                        callback.onResult(it, it.first().basic.registeredTimestamp, it.last().basic.registeredTimestamp)
                    }
                    retryFunc = null
                    initialLoadState.postValue(LoadState.LOADED)
                    loadState.postValue(LoadState.LOADED)
                }

            } catch (e: Exception) {
                retryFunc = {
                    loadInitial(params, callback)
                }
                initialLoadState.postValue(LoadState.ERROR(e))
                loadState.postValue(LoadState.ERROR(e))
            }
        }

        override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Long, ExperimentProtos.ExperimentEssential>) {
            loadState.postValue(LoadState.LOADING)
            try {
                GrpcApi.listExperiments(toTimestamp = params.key, limit = params.requestedLoadSize).let {
                    if(it.isEmpty()) {
                        callback.onResult(it, null)
                    } else {
                        callback.onResult(it, it.last().basic.registeredTimestamp)
                    }
                }
                retryFunc = null
                loadState.postValue(LoadState.LOADED)
            } catch (e: Exception) {
                retryFunc = {
                    loadAfter(params, callback)
                }
                loadState.postValue(LoadState.ERROR(e))
            }
        }

        override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Long, ExperimentProtos.ExperimentEssential>) {}

        class Factory : DataSource.Factory<Long, ExperimentProtos.ExperimentEssential>() {
            val sourceLiveData = MutableLiveData<ExperimentDataSource>()

            override fun create(): DataSource<Long, ExperimentProtos.ExperimentEssential> {
                val source = ExperimentDataSource()

                sourceLiveData.postValue(source)
                return source
            }
        }
    }

    class ExperimentDataViewModel : ViewModel() {
        private val factory = ExperimentDataSource.Factory()

        val pagedList = LivePagedListBuilder(factory, 20)
            .setFetchExecutor(Executors.newCachedThreadPool())
            .build()

        val loadState: LiveData<LoadState> = Transformations.switchMap(factory.sourceLiveData) {it.loadState }

        val initialLoadState : LiveData<LoadState> = Transformations.switchMap(factory.sourceLiveData) { it.initialLoadState }

        fun retry(executor: Executor) {
            factory.sourceLiveData.value?.retry(executor)
        }

        fun refresh() {
            factory.sourceLiveData.value?.invalidate()
        }
    }

    class ViewHolder(view: ExperimentItemView, onClick: (position: Int, view: View) -> Unit) : BaseViewHolder<ExperimentItemView, ExperimentProtos.ExperimentEssential>(view, onClick){
        override fun bindView(data: ExperimentProtos.ExperimentEssential) {
            view.bindView(data)
        }

        override fun clearView() {
            view.clear()
        }
    }
}