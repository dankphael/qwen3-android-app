package com.example.qwen3chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.qwen3chat.databinding.ItemCarouselCardBinding
import kotlin.math.abs

class CarouselAdapter(
    private val cards: List<CarouselCard>
) : RecyclerView.Adapter<CarouselAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCarouselCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size

    class CardViewHolder(
        private val binding: ItemCarouselCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: CarouselCard) {
            binding.textViewIcon.text = card.icon
            binding.textViewTitle.text = card.title
            binding.textViewDescription.text = card.description
        }
    }

    /** Scale + alpha PageTransformer for carousel effect */
    class CarouselScaleTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val absPos = abs(position)
            page.apply {
                scaleX = 1f - (0.08f * absPos)
                scaleY = 1f - (0.08f * absPos)
                alpha = 1f - (0.4f * absPos)
            }
        }
    }
}
