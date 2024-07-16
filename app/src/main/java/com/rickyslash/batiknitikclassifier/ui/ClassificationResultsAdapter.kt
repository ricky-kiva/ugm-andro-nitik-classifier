package com.rickyslash.batiknitikclassifier.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rickyslash.batiknitikclassifier.R
import com.rickyslash.batiknitikclassifier.databinding.ItemClassificationResultBinding
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.Classifications
import kotlin.math.min

class ClassificationResultsAdapter
    : RecyclerView.Adapter<ClassificationResultsAdapter.ViewHolder>() {

    private var predictions: MutableList<Category?> = mutableListOf()
    private var adapterSize: Int = 0

    fun updateResults(newPredictions: List<Classifications>?) {
        predictions = MutableList(adapterSize) { null }

        newPredictions?.let { newPred ->
            if (newPred.isNotEmpty()) {
                val sortedCategories = newPred[0].categories.sortedByDescending { it.score }
                val min = min(sortedCategories.size, predictions.size)

                for (i in 0 until  min) {
                    predictions[i] = sortedCategories[i]
                }
            }
        }
    }

    fun updateAdapterSize(size: Int) {
        adapterSize = size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassificationResultBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        predictions[position].let { pred ->
            holder.bind(pred?.label, pred?.score)
        }
    }

    override fun getItemCount(): Int = predictions.size

    inner class ViewHolder(private val binding: ItemClassificationResultBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: String?, score: Float?) {
            with (binding) {
                val noValue = root.context
                    .getString(R.string.val_classification_result_default)

                tvLabel.text = label ?: noValue
                tvScore.text = score?.let { "%.2f".format(it) } ?: noValue
            }
        }
    }
}